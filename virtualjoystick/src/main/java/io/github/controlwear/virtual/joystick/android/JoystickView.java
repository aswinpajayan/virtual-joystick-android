package io.github.controlwear.virtual.joystick.android;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View
        implements
        Runnable {


    // INTERFACE
    public interface OnJoystickListener {

        void onMove(int angle, int strength);
    }


    // CONSTANTS
    private static final int DEFAULT_LOOP_INTERVAL = 50; // in milliseconds


    private static final int DEFAULT_COLOR = Color.BLACK;
    private static final int DEFAULT_BACKGROUND_COLOR = Color.TRANSPARENT;
    private static final int DEFAULT_SIZE = 200;
    private static final double RATIO_SIZE_BUTTON = 0.25;
    private static final double RATIO_SIZE_BORDER = 0.75;

    private static final int DEFAULT_WIDTH_BORDER = 3;


    // DRAWING
    private Paint mPaintCircleButton;
    private Paint mPaintCircleBorder;
    private Paint mPaintBackground;

    private int mButtonColor;
    private int mBorderColor;
    private int mBackgroundColor;

    // COORDINATE
    private int mPosX = 0;
    private int mPosY = 0;
    private int mCenterX = 0;
    private int mCenterY = 0;

    // SIZE
    private int mButtonRadius;
    private int mBorderRadius;
    private int mBorderWidth;


    private OnJoystickListener mCallback;
    private long mLoopInterval = DEFAULT_LOOP_INTERVAL;
    private Thread mThread = new Thread(this);


    /*
    CONSTRUCTORS
     */


    public JoystickView(Context context) {
        this(context, null);
    }


    public JoystickView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, null);
    }


    /**
     * Constructor, try to get the attributes or use the default values and setup the paints accordingly.
     * @param context Context
     * @param attrs Attributes, useful to customize the view
     */
    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.JoystickView,
                0, 0
        );

        try {
            mButtonColor = styledAttributes.getColor(R.styleable.JoystickView_buttonColor, DEFAULT_COLOR);
            mBorderColor = styledAttributes.getColor(R.styleable.JoystickView_borderColor, DEFAULT_COLOR);
            mBackgroundColor = styledAttributes.getColor(R.styleable.JoystickView_backgroundColor, DEFAULT_BACKGROUND_COLOR);
            mBorderWidth = styledAttributes.getDimensionPixelSize(R.styleable.JoystickView_borderWidth, DEFAULT_WIDTH_BORDER);
        } finally {
            styledAttributes.recycle();
        }

        setupPaint();
    }


    /**
     * Initialize the drawing according to some attributes
     */
    protected void setupPaint() {

    }


    @Override
    protected void onDraw(Canvas canvas) {
        // Draw the background
        canvas.drawCircle(mCenterX, mCenterY, mBorderRadius, mPaintBackground);

        // Draw the circle border
        canvas.drawCircle(mCenterX, mCenterY, mBorderRadius, mPaintCircleBorder);

        // Draw the circle button
        canvas.drawCircle(mPosX, mPosY, mButtonRadius, mPaintCircleButton);
    }


    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        // get the center of view to position circle
        mCenterX = mPosX = getWidth() / 2;
        mCenterY = mPosY = getWidth() / 2;

        // radius based on smallest size : height OR width
        int d = Math.min(xNew, yNew);
        mButtonRadius = (int) (d / 2 * RATIO_SIZE_BUTTON);
        mBorderRadius = (int) (d / 2 * RATIO_SIZE_BORDER);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // setting the measured values to resize the view to a certain width and height
        int d = Math.min(measure(widthMeasureSpec), measure(heightMeasureSpec));
        setMeasuredDimension(d, d);
    }


    private int measure(int measureSpec) {
        if (MeasureSpec.getMode(measureSpec) == MeasureSpec.UNSPECIFIED) {
            // if no bounds are specified return a default size (200)
            return DEFAULT_SIZE;
        } else {
            // As you want to fill the available space
            // always return the full available bounds.
            return MeasureSpec.getSize(measureSpec);
        }
    }


    /*
    USER EVENT
     */


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // to move the button according to the finger coordinate
        mPosX = (int) event.getX();
        mPosY = (int) event.getY();

        double abs = Math.sqrt((mPosX - mCenterX) * (mPosX - mCenterX)
                + (mPosY - mCenterY) * (mPosY - mCenterY));

        // move the button only within its boundaries
        if (abs > mBorderRadius) {
            mPosX = (int) ((mPosX - mCenterX) * mBorderRadius / abs + mCenterX);
            mPosY = (int) ((mPosY - mCenterY) * mBorderRadius / abs + mCenterY);
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            mPosX = mCenterX;
            mPosY = mCenterY;

            mThread.interrupt();

            if (mCallback != null)
                mCallback.onMove(getAngle(), getStrength());
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mThread != null && mThread.isAlive()) {
                mThread.interrupt();
            }

            mThread = new Thread(this);
            mThread.start();

            if (mCallback != null)
                mCallback.onMove(getAngle(), getStrength());
        }

        // to force a new draw
        invalidate();

        return true;
    }


    /**
     * Process the angle following the 360° counter-clock protractor rules.
     * @return the angle of the button
     */
    private int getAngle() {
        int angle = (int) Math.toDegrees(Math.atan2(mCenterY - mPosY, mPosX - mCenterX));
        return angle < 0 ? angle + 360 : angle; // make it as a regular counter-clock protractor
    }


    /**
     * Process the strength as a percentage of the distance between the center and the border.
     * @return the strength of the button
     */
    private int getStrength() {
        return (int) (100 * Math.sqrt((mPosX - mCenterX)
                * (mPosX - mCenterX) + (mPosY - mCenterY)
                * (mPosY - mCenterY)) / mBorderRadius);
    }


    /*
    SETTERS
     */


    public void setButtonColor(int newColor){
        mButtonColor=newColor;
    }


    public void setBorderColor(int newColor){
        mBorderColor=newColor;
    }


    @Override
    public void setBackgroundColor(int newColor) {
        mBackgroundColor = newColor;
    }


    public void setBorderWidth(int newWidth) {
        mBorderWidth = newWidth;
    }


    public void setOnJoystickListener(OnJoystickListener listener) {
        setOnJoystickListener(listener, DEFAULT_LOOP_INTERVAL);
    }


    public void setOnJoystickListener(OnJoystickListener listener, int loopInterval) {
        mCallback = listener;
        mLoopInterval = loopInterval;
    }


    /*
    IMPLEMENTS
     */


    @Override // Runnable
    public void run() {
        while (!Thread.interrupted()) {
            post(new Runnable() {
                public void run() {
                    if (mCallback != null)
                        mCallback.onMove(getAngle(), getStrength());
                }
            });

            try {
                Thread.sleep(mLoopInterval);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}