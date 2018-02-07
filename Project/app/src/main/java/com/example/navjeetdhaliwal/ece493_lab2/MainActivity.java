package com.example.navjeetdhaliwal.ece493_lab2;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.security.auth.login.LoginException;

public class MainActivity extends AppCompatActivity implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private ProgressDialog progress;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int PICK_IMAGE = 2;
    int EDGE_SCREEN_THRESHOLD;
    private Button takePictureButton;
    private ImageView imageView;
    ImageProcessor iProcessor;
    GestureDetectorCompat gDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_view);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        takePictureButton = findViewById(R.id.button_image);
        imageView = findViewById(R.id.imageview);
        progress= new ProgressDialog(this);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        iProcessor = new ImageProcessor(2);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        EDGE_SCREEN_THRESHOLD = displayMetrics.widthPixels/6;

        this.gDetector = new GestureDetectorCompat(this,this);
        gDetector.setOnDoubleTapListener(this);

        //check if we have camera permissions, otherwise grab permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            takePictureButton.setEnabled(false);
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Number of Undo");
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setRawInputType(Configuration.KEYBOARD_12KEY);
                alert.setView(input);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        iProcessor.setNumUndo(Integer.parseInt(input.getText().toString()));
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //Put actions for CANCEL button here, or leave in blank
                    }
                });
                alert.show();
                return true;


            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.gDetector.onTouchEvent(event);
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    public void takePicture(View view) {
        Log.d("Camera", "Button pressed");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            Log.d("Camera", "not null");


            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.navjeetdhaliwal.ece493_lab2.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                Log.d("Camera", "Activity Started for result");
            }

        }
    }
    public void openGallery(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    // Grabbing camera permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                takePictureButton.setEnabled(true);
            }
        }
    }


    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(iProcessor.getCurrentImagePath());
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }


    //Result for dispatchTakePictureIntent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if  (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            iProcessor.loadNewImage(BitmapFactory.decodeFile(iProcessor.getCurrentImagePath()));
            imageView.setImageBitmap(iProcessor.getCurrentImage());

            galleryAddPic();

        }else if(requestCode == PICK_IMAGE && resultCode == RESULT_OK){
            Uri targetUri = data.getData();
            try {

                iProcessor.loadNewImage(BitmapFactory.decodeStream(getContentResolver().openInputStream(targetUri)));
                imageView.setImageBitmap(iProcessor.getCurrentImage());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }



    }

    //create image file for camera to save to
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                getResources().getString(R.string.app_name));
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file path for use with ACTION_VIEW intents
        String imagePath = image.getAbsolutePath();
        iProcessor.setCurrentImagePath(imagePath);
        return image;
    }

    public void saveFile( View view){
        if (iProcessor.imageExists()) {
            try {
                File cachePath = createImageFile();
                //cachePath.createNewFile();
                FileOutputStream ostream = new FileOutputStream(cachePath);
                iProcessor.getCurrentImage().compress(Bitmap.CompressFormat.JPEG, 100, ostream);
                ostream.close();
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(cachePath);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d("Saving", "Image Saved");
            //Toast.makeText(this, "Current Image Saved", Toast.LENGTH_SHORT).show();
        }else {
            Log.d("Saving", "No Image to Save");
            //Toast.makeText(this, "No Image to Save", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean onDown(MotionEvent event) {
        //Toast.makeText(this, "onDown", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        //if (Math.abs(event1.getX() - event2.getX()) > SWIPE_MAX_OFF_PATH){
          //  return false;
        //}
        //undo swipe from left edge
        if(event2.getX() - event1.getX() > SWIPE_MIN_DISTANCE
                && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY
                && event1.getX() < EDGE_SCREEN_THRESHOLD) {
            Log.d("UNDO","Swiped Undo");
            iProcessor.undoBitmap();
            imageView.setImageBitmap(iProcessor.getCurrentImage());

        } else if (event1.getY() - event2.getY() > SWIPE_MIN_DISTANCE
                && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
            if(iProcessor.imageExists()) {
                progress.setMessage("Loading...");
                progress.setTitle("Inverting Image");
                progress.show();
                progress.setCancelable(false);
                new Thread(new Runnable() {
                    public void run() {
                        // a potentially  time consuming task
                        iProcessor.invertImage();

                        imageView.post(new Runnable() {
                            public void run() {
                                imageView.setImageBitmap(iProcessor.getCurrentImage());
                                progress.dismiss();
                            }
                        });
                    }
                }).start();
            }


        }
        // swipe down
        else if (event2.getY() - event1.getY() > SWIPE_MIN_DISTANCE
                && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
            if(iProcessor.imageExists()) {
                progress.setMessage("Loading...");
                progress.setTitle("Blurring Image");
                progress.show();
                progress.setCancelable(false);
                new Thread(new Runnable() {
                    public void run() {
                        // a potentially  time consuming task
                        iProcessor.blurImage();

                        imageView.post(new Runnable() {
                            public void run() {
                                imageView.setImageBitmap(iProcessor.getCurrentImage());
                                progress.dismiss();
                            }
                        });
                    }
                }).start();
            }

        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        if(iProcessor.imageExists()) {
            progress.setMessage("Loading...");
            progress.setTitle("Bulging Image");
            progress.show();
            progress.setCancelable(false);
            new Thread(new Runnable() {
                public void run() {
                    // a potentially  time consuming task
                    iProcessor.bulgeImage();

                    imageView.post(new Runnable() {
                        public void run() {
                            imageView.setImageBitmap(iProcessor.getCurrentImage());
                            progress.dismiss();
                        }
                    });
                }
            }).start();
        }
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2,
                            float distanceX, float distanceY) {

        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        //Toast.makeText(this, "onShowPress", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        //Toast.makeText(this, "onSingleTapUp", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        if(iProcessor.imageExists()) {
            progress.setMessage("Loading...");
            progress.setTitle("Swirling Image");
            progress.show();
            progress.setCancelable(false);
            new Thread(new Runnable() {
                public void run() {
                    // a potentially  time consuming task
                    iProcessor.swirlImage();

                    imageView.post(new Runnable() {
                        public void run() {
                            imageView.setImageBitmap(iProcessor.getCurrentImage());
                            progress.dismiss();
                        }
                    });
                }
            }).start();

        }

        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        //Toast.makeText(this, "onDoubleTapEvent", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        //Toast.makeText(this, "onSingleTapConfirmed", Toast.LENGTH_SHORT).show();
        return true;
    }


}
