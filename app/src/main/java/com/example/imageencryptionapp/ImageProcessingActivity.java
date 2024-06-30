package com.example.imageencryptionapp;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;

import android.content.Context;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class ImageProcessingActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imageView;
    private Bitmap originalBitmap;
    private byte[] encryptedImage;
    private byte[] decryptedImage;
    private byte[] encryptedAESKey;
    private PrivateKey privateKey;
    private Bitmap encryptedBitmap;  // Declaration added
    private Bitmap decryptedBitmap;  // Declaration added

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_processing);

        imageView = findViewById(R.id.imageView);
        Button selectImageButton = findViewById(R.id.selectImageButton);
        Button encryptButton = findViewById(R.id.encryptButton);
        Button decryptButton = findViewById(R.id.decryptButton);

        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        encryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (originalBitmap != null) {
                    // Encryption logic
                    encryptImage(originalBitmap);
                } else {
                    showToast("Please select an image first");
                }
            }
        });

        decryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (encryptedImage != null) {
                    // Decryption logic
                    decryptImage();
                } else {
                    showToast("No encrypted image to decrypt");
                }
            }
        });

        Button textProcessingButton = findViewById(R.id.textProcessingButton);
        textProcessingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ImageProcessingActivity.this, TextProcessingActivity.class);
                startActivity(intent);
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                originalBitmap = BitmapFactory.decodeStream(inputStream);
                imageView.setImageBitmap(originalBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void encryptImage(Bitmap originalBitmap) {
        try {
            // Convert Bitmap to byte array
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] imageBytes = stream.toByteArray();

            // Scale down the image
            encryptedBitmap = decodeSampledBitmapFromByteArray(imageBytes, 300, 300); // Adjust the size as needed

            // Generate RSA key pair
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            PublicKey publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();

            // Generate AES key
            KeyGenerator aesKeyGenerator = KeyGenerator.getInstance("AES");
            aesKeyGenerator.init(256);
            SecretKey aesKey = aesKeyGenerator.generateKey();

            // Encrypt AES key with RSA public key
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            encryptedAESKey = rsaCipher.doFinal(aesKey.getEncoded());

            // Encrypt image using AES
            Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
            encryptedImage = aesCipher.doFinal(imageBytes);

            // Log statement to check the length of the encrypted image array
            Log.d("Encryption", "Encrypted image length: " + encryptedImage.length);

            // Show the encrypted image in the image view container
            imageView.setImageBitmap(encryptedBitmap);
            showToast("Image Encrypted Successfully");

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Encryption failed");
        }
    }


    private Bitmap decodeSampledBitmapFromByteArray(byte[] data, int reqWidth, int reqHeight) {
        // Decode the image without scaling
        Bitmap originalBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

        // Create a blurred version of the bitmap
        Bitmap blurredBitmap = applyBlur(originalBitmap, this);

        return blurredBitmap;
    }

    private Bitmap applyBlur(Bitmap image, Context context) {
        // Let's scale the image down to improve performance
        int width = Math.round(image.getWidth() * 0.2f);
        int height = Math.round(image.getHeight() * 0.2f);

        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);

        theIntrinsic.setRadius(25f); // Adjust this value for the blur intensity
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        return outputBitmap;
    }


    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private void decryptImage() {
        try {
            // Decryption logic
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedAESKey = rsaCipher.doFinal(encryptedAESKey);

            Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            aesCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptedAESKey, "AES"));
            decryptedImage = aesCipher.doFinal(encryptedImage);

            // Show the decrypted image in the image view container
            decryptedBitmap = BitmapFactory.decodeByteArray(decryptedImage, 0, decryptedImage.length);
            imageView.setImageBitmap(decryptedBitmap);

            showToast("Image Decrypted Successfully");

            // Store the decrypted image in the gallery
            saveToGallery(decryptedBitmap);

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Decryption failed");
        }
    }

    private void saveToGallery(Bitmap bitmap) {
        String title = "Decrypted_Image_" + System.currentTimeMillis();
        String description = "Image decrypted using RSA and AES";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DESCRIPTION, description);

        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (imageUri != null) {
            try {
                OutputStream outputStream = getContentResolver().openOutputStream(imageUri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                if (outputStream != null) {
                    outputStream.close();
                }
                showToast("Image saved to gallery");
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Failed to save image");
            }
        } else {
            showToast("Failed to save image");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
