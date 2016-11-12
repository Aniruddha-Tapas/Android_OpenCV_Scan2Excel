package jp.narr.tensorflowmnist;

import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Puzzle15Activity extends AppCompatActivity implements CvCameraViewListener, View.OnTouchListener {

    private static final String TAG = "Puzzle15";

    private CameraBridgeViewBase mOpenCvCameraView;
    private Puzzle15Processor mPuzzle15;
    private MenuItem takePic;
    private MenuItem mItemHideGrids;
    private MenuItem mItemHideNumbers;
    private Mat frame;
    private Bitmap bitmap;
    private Bitmap[] cellbitmap;
    private boolean isCameraViewStarted = false;

    private int mGameWidth;
    private int mGameHeight;

    int [] pixels;
    double[] d;
    private DigitDetector mDetector = new DigitDetector();

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Now enable camera view to start receiving frames
                    mOpenCvCameraView.setOnTouchListener(Puzzle15Activity.this);
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    //Static OpenCV init
    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "OpenCV initialization Failed");
        } else {
            Log.d("OpenCV", "OpenCV initialization Succeeded");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.d(TAG, "Creating and setting view");
        //mOpenCvCameraView = (CameraBridgeViewBase) new JavaCameraView(this, -1);
        mOpenCvCameraView = (CameraBridgeViewBase) new JavaCameraView(this, -1);
        setContentView(mOpenCvCameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mPuzzle15 = new Puzzle15Processor();
        mPuzzle15.prepareNewGame();


        boolean ret = mDetector.setup(this);
        if( !ret ) {
            Log.i(TAG, "Detector setup failed");
            return;
        }


    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            //mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        takePic = menu.add("Take Picture");
        takePic.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mItemHideNumbers = menu.add("Show/hide tile numbers");
        mItemHideGrids = menu.add("Show/hide grid lines");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "Menu Item selected " + item);
        if (item == takePic) {
            /* Save the current picture */
            if(!isCameraViewStarted) {
                Log.d(TAG, "isCameraViewStarted: " + "Camera started");

                if(mPuzzle15 != null){
                    Log.d(TAG, mPuzzle15.getFrame().toString());
                }
                else{
                    Log.d(TAG, "mPuzzle15.getFrame() is null");
                }

                savePic(mPuzzle15.getFrame());
                saveCells(mPuzzle15.getCells());

            }
            else
                Log.d(TAG, "isCameraViewStarted: " + "Camera not started");


        } else if (item == mItemHideGrids) {
            /* We need to start new game */
            mPuzzle15.toggleGridLines();
        } else if (item == mItemHideNumbers) {
            /* We need to enable or disable drawing of the tile numbers */
            mPuzzle15.toggleTileNumbers();
        } else if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return true;
    }

    private void savePic(Mat mat) {
        String fileName;
        File folder = new File(Environment.getExternalStorageDirectory().toString()
                + "/Scan2Excel");
        if (!folder.exists()) {
            folder.mkdir();
            Log.d(TAG, "wrote: created folder " + folder.getPath());
        }

        File folder2 = new File(Environment.getExternalStorageDirectory().toString()
                + "/Scan2Excel/Saved_Images");
        if (!folder2.exists()) {
            folder2.mkdir();
            Log.d(TAG, "wrote: created folder2 " + folder2.getPath());
        }

        fileName = Environment.getExternalStorageDirectory().toString()
                + "/Scan2Excel/Saved_Images/Image-"
                + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())
                + "["
                + "]"
                + ".jpg";


        Mat endDoc = new Mat(Double.valueOf(mat.size().width).intValue(),
                Double.valueOf(mat.size().height).intValue(), CvType.CV_8UC4);

        Core.flip(mat.t(), endDoc, 1);

        Imgcodecs.imwrite(fileName, endDoc);
        endDoc.release();

        Toast.makeText(this,"Saved Main Mat",Toast.LENGTH_LONG).show();

        /*
        File file = new File(folder2, fileName);
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }

    private void saveCells(Mat[] cells) {

        for (int i=0; i < cells.length; i++) {

            Mat endDoc = new Mat(Double.valueOf(cells[i].size().width).intValue(),
                    Double.valueOf(cells[i].size().height).intValue(), CvType.CV_8UC4);

            Bitmap bmp = null;
			try{
                bmp = Bitmap.createBitmap(endDoc.cols(),endDoc.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(endDoc,bmp);
            }catch (CvException e){Log.d(TAG, "CvException =" + e.getMessage());}

            int width = bmp.getWidth();
            int height = bmp.getHeight();

            // Get 28x28 pixel data from bitmap
            int[] pixels = new int[width * height];
            bmp.getPixels(pixels, 0, width, 0, 0, width, height);

            int[] retPixels = new int[pixels.length];
            for (int p = 0; p < pixels.length; ++p) {
                // Set 0 for white and 255 for black pixel
                int pix = pixels[p];
                int b = pix & 0xff;
                retPixels[p] = 0xff - b;
            }


            for (int id=0; id< retPixels.length; id++){
                //pixels[id] = (int) d[id];
                Log.i("PIXEL_ARRAY", " =" + retPixels[id]);
            }

            int digit = mDetector.detectDigit(retPixels);

            Log.i(TAG, "digit =" + digit);


            String fileName;
            File folder = new File(Environment.getExternalStorageDirectory().toString()
                    + "/Scan2Excel");
            if (!folder.exists()) {
                folder.mkdir();
                Log.d(TAG, "wrote: created folder " + folder.getPath());
            }

            File folder2 = new File(Environment.getExternalStorageDirectory().toString()
                    + "/Scan2Excel/Saved_Images");
            if (!folder2.exists()) {
                folder2.mkdir();
                Log.d(TAG, "wrote: created folder2 " + folder2.getPath());
            }

            fileName = Environment.getExternalStorageDirectory().toString()
                    + "/Scan2Excel/Saved_Images/Image-"
                    + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())
                    +"-Cell-"
                    + "["
                    + i
                    + "]"
                    + ".jpg";

            Core.flip(cells[i].t(), endDoc, 1);

            Imgcodecs.imwrite(fileName, endDoc);
            endDoc.release();
        }

        Toast.makeText(this,"Saved Cells",Toast.LENGTH_LONG).show();

    }


    public void onCameraViewStarted(int width, int height) {
        mGameWidth = width;
        mGameHeight = height;
        mPuzzle15.prepareGameSize(width, height);
    }

    public void onCameraViewStopped() {
    }


    public boolean onTouch(View view, MotionEvent event) {
        int xpos, ypos;

        xpos = (view.getWidth() - mGameWidth) / 2;
        xpos = (int) event.getX() - xpos;

        ypos = (view.getHeight() - mGameHeight) / 2;
        ypos = (int) event.getY() - ypos;

        Log.d(TAG, " XPOS: " + xpos + " YPOS: " + ypos);

        /*
        if (xpos >=0 && xpos <= mGameWidth && ypos >=0  && ypos <= mGameHeight) {
            // click is inside the picture. Deliver this event to processor
            mPuzzle15.deliverTouchEvent(xpos, ypos);
        }
        */

        return false;
    }


    public Mat onCameraFrame(Mat inputFrame) {
        return mPuzzle15.puzzleFrame(inputFrame);
    }
}
