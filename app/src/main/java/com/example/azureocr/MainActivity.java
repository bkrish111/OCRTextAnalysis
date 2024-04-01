package com.example.azureocr;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private final String azureEndpoint = "https://OCRTextAnalysis.cognitiveservices.azure.com/computervision/imageanalysis:analyze/"; // Replace with your Azure endpoint URL
    private final String subscriptionKey = "67740ac02048464a903ecdc1368eaa1f"; // Replace with your subscription key


    private ImageView imageView;
    private TextView tvResults;
    private String selectedImagePath;

    Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnSelectImage = findViewById(R.id.btnSelectImage);
        Button btnAnalyze = findViewById(R.id.btnAnalyze);
        tvResults = findViewById(R.id.tvResults);
        imageView = findViewById(R.id.imageView);


        btnSelectImage.setOnClickListener(view -> openImagePicker());

        btnAnalyze.setOnClickListener(view -> {
            if (selectedImageUri != null ) {
                analyzeImage(selectedImageUri);
            } else {
                tvResults.setText("No image selected.");
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            selectedImagePath = getPathFromUri(selectedImageUri);
            imageView.setImageURI(selectedImageUri); // Display the selected image
        }
    }

    public String getPathFromUri(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }

    private byte[] uriToByteArray(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((inputStream != null) && (length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Handle this error condition appropriately
        }
    }
    private void analyzeImage(Uri selectedImageUri) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(azureEndpoint)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        AzureCognitiveServiceAPI service = retrofit.create(AzureCognitiveServiceAPI.class);
        byte[] imageData = uriToByteArray(selectedImageUri);
        if (imageData == null) {
            tvResults.setText("Error converting image to byte array.");
            return;
        }
        String apiVersion = "2023-02-01-preview";
        String features = "read";
        String language = "en";
        boolean genderNeutralCaption = false;
        RequestBody reqFile = RequestBody.create(MediaType.parse("application/octet-stream"), imageData);
        Call<OCRResult> call = service.analyzeImage(
                azureEndpoint,
                subscriptionKey,
                "application/octet-stream",
                reqFile,
                apiVersion,
                features,
                language,
                genderNeutralCaption);
        call.enqueue(new Callback<OCRResult>() {
            @Override
            public void onResponse(Call<OCRResult> call, Response<OCRResult> response) {
                if (response.isSuccessful() && response.body() != null) {
                    OCRResult ocrResponse = response.body();
                    // Assuming OCRResponse class has a toString() method or similar to format its content
                    String resultText = "OCR Result: " + ocrResponse.getReadResult().getContent();
                    tvResults.setText(resultText);
                } else {
                    tvResults.setText("OCR failed with response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<OCRResult> call, Throwable t) {
                tvResults.setText("OCR request failed: " + t.getMessage());
                System.out.println(t.getMessage());
            }
        });
    }
}

