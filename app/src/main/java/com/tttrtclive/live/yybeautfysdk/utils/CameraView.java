package com.tttrtclive.live.yybeautfysdk.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private static final String TAG = "CameraView";

    private CameraUtil mCamera;
    private GLTexture mOESTexture;
    private SurfaceTexture mSurfaceTexture;
    private boolean mCameraOpening;
    private QuadRenderer mQuadRendererOES;
    private QuadRenderer mQuadRenderer;
    private DrawFrameCallback mDrawFrameCallback;
    private int mViewWidth;
    private int mViewHeight;
    private int mRenderWidth;
    private int mRenderHeight;
    private GLTexture mTextureIn;
    private GLTexture mTextureOut;
    private int mFrameBuffer;
    private float[] mIdentityMatrix;
    private float[] mFlipYMatrix;
    private ByteBuffer mTextureOutBuffer;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mCamera = new CameraUtil((Activity) getContext());
        mRenderWidth = 0;
        mRenderHeight = 0;
        mFrameBuffer = 0;

        mIdentityMatrix = new float[16];
        Matrix.setIdentityM(mIdentityMatrix, 0);

        mFlipYMatrix = new float[16];
        Matrix.setIdentityM(mFlipYMatrix, 0);
        Matrix.scaleM(mFlipYMatrix, 0, 1f, -1, 1);

        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setEGLContextClientVersion(3);

        setPreserveEGLContextOnPause(true);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private void release() {
        Log.i(TAG, "release");

        if (mDrawFrameCallback != null) {
            mDrawFrameCallback.onRelease();
        }

        if (mOESTexture != null) {
            mOESTexture.release();
            mOESTexture = null;

            mSurfaceTexture.release();
            mSurfaceTexture = null;

            mQuadRendererOES.release();
            mQuadRendererOES = null;

            mQuadRenderer.release();
            mQuadRenderer = null;

            if (mTextureIn != null) {
                mTextureIn.release();
                mTextureIn = null;
            }

            if (mTextureOut != null) {
                mTextureOut.release();
                mTextureOut = null;
            }

            if (mFrameBuffer != 0) {
                GLES20.glDeleteFramebuffers(1, new int[] { mFrameBuffer }, 0);
                mFrameBuffer = 0;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mCamera.onResume();
    }

    @Override
    public void onPause() {
        mCamera.onPause();

        queueEvent(new Runnable() {
            @Override
            public void run() {
                release();
            }
        });

        super.onPause();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.i(TAG, "onSurfaceCreated GLES Version: " + GLES20.glGetString(GLES20.GL_VERSION));
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int w, int h) {
        Log.i(TAG, "onSurfaceChanged: " + w + " " + h);

        mViewWidth = w;
        mViewHeight = h;

        if (mOESTexture == null) {
            mOESTexture = new GLTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);

            mSurfaceTexture = new SurfaceTexture(mOESTexture.getTextureId());
            mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    CameraView.this.requestRender();
                    mCameraOpening = false;
                }
            });

            mCameraOpening = false;
            mQuadRendererOES = new QuadRenderer(true);
            mQuadRenderer = new QuadRenderer(false);

            if (mRenderWidth > 0 && mRenderHeight > 0) {
                if (mTextureIn == null) {
                    mTextureIn = new GLTexture(GLES20.GL_TEXTURE_2D);
                    mTextureIn.texImage(mRenderWidth, mRenderHeight);
                }
                if (mTextureOut == null) {
                    mTextureOut = new GLTexture(GLES20.GL_TEXTURE_2D);
                    mTextureOut.texImage(mRenderWidth, mRenderHeight);
                }

                int[] fbos = new int[1];
                GLES20.glGenFramebuffers(1, fbos, 0);
                mFrameBuffer = fbos[0];
            }

            if (mDrawFrameCallback != null) {
                mDrawFrameCallback.onInit();
            }
        }

        if (!mCameraOpening) {
            mCameraOpening = true;

            mCamera.openCamera(CameraUtil.FRONT_CAMERA, mRenderWidth, mRenderHeight, mSurfaceTexture);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        mSurfaceTexture.updateTexImage();

        float[] transform = new float[16];
        mSurfaceTexture.getTransformMatrix(transform);
        transformMatrix(transform);

        if (mDrawFrameCallback != null) {
            // get default fbo
            int[] fbos = new int[1];
            GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, fbos, 0);
            int defaultFBO = fbos[0];

            // draw oes texture to in texture
            bindFrameBuffer(mFrameBuffer, mTextureIn);
            setViewportAndClear(mTextureIn.getWidth(), mTextureIn.getHeight());

            float[] transformFlipY = new float[16];
            Matrix.multiplyMM(transformFlipY, 0, mFlipYMatrix, 0, transform, 0);
            mQuadRendererOES.draw(transformFlipY, mOESTexture.getTextureId());

            // read in pixels
            GLES20.glFinish();
            bindFrameBuffer(mFrameBuffer, mTextureIn);
            if (mTextureOutBuffer == null || mTextureOutBuffer.capacity() < mTextureIn.getWidth() * mTextureIn.getHeight() * 4) {
                mTextureOutBuffer = ByteBuffer.allocate(mTextureIn.getWidth() * mTextureIn.getHeight() * 4);
            }
            GLES20.glReadPixels(0, 0, mTextureIn.getWidth(), mTextureIn.getHeight(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mTextureOutBuffer);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, defaultFBO);

            // apply draw callback
            mDrawFrameCallback.onDrawFrame(mTextureOutBuffer.array(), mTextureIn, mTextureOut);

            // draw out texture to surface view
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, defaultFBO);
            setViewportAndClear(mViewWidth, mViewHeight);
            mQuadRenderer.draw(mIdentityMatrix, mTextureOut.getTextureId());

            // read out pixels
            bindFrameBuffer(mFrameBuffer, mTextureOut);
            if (mTextureOutBuffer == null || mTextureOutBuffer.capacity() < mTextureOut.getWidth() * mTextureOut.getHeight() * 4) {
                mTextureOutBuffer = ByteBuffer.allocate(mTextureOut.getWidth() * mTextureOut.getHeight() * 4);
            }
            GLES20.glReadPixels(0, 0, mTextureOut.getWidth(), mTextureOut.getHeight(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mTextureOutBuffer);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, defaultFBO);

            mDrawFrameCallback.onOutputFrame(mTextureOutBuffer.array(), mTextureOut.getWidth(), mTextureOut.getHeight());
        } else {
            // draw oes texture to surface view
            setViewportAndClear(mViewWidth, mViewHeight);
            mQuadRendererOES.draw(transform, mOESTexture.getTextureId());
        }
    }

    public int getCameraOrientation() {
        return mCamera.getSensorOrientation();
    }

    public void touchFocus(PointF pos, int size) {
        mCamera.touchFocus(pos, size);
    }

    public void copyTexture(GLTexture textureIn, GLTexture textureOut) {
        bindFrameBuffer(mFrameBuffer, textureOut);
        setViewportAndClear(textureOut.getWidth(), textureOut.getHeight());
        mQuadRenderer.draw(mFlipYMatrix, textureIn.getTextureId());
    }

    private void setViewportAndClear(int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }

    private void bindFrameBuffer(int fbo, GLTexture texture) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, texture.getTarget(), texture.getTextureId(), 0);

        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "bindFrameBuffer failed status: " + status);
        }
    }

    private void multiplyMatrixRight(float[] left, float[] right) {
        float[] result = new float[16];
        Matrix.multiplyMM(result, 0, left, 0, right, 0);
        System.arraycopy(result, 0, right, 0, result.length);
    }

    private void transformMatrix(float[] transform) {
        float[] result = new float[16];
        Matrix.invertM(result, 0, transform, 0);

        result[12] = 0;
        result[13] = 0;
        result[14] = 0;

        float[] flipX = new float[16];
        Matrix.setIdentityM(flipX, 0);
        Matrix.scaleM(flipX, 0, -1f, 1, 1);

        multiplyMatrixRight(flipX, result);

        System.arraycopy(result, 0, transform, 0, result.length);
    }

    public interface DrawFrameCallback {
        void onDrawFrame(byte[] data, GLTexture textureIn, GLTexture textureOut);
        void onOutputFrame(byte[] data, int width, int height);
        void onInit();
        void onRelease();
    }

    public void setDrawFrameCallback(int renderWidth, int renderHeight, @Nullable DrawFrameCallback callback) {
        mRenderWidth = renderWidth;
        mRenderHeight = renderHeight;
        mDrawFrameCallback = callback;
    }

    public void setImageReadCallback(@Nullable CameraUtil.ImageReadCallback callback) {
        mCamera.setImageReadCallback(callback);
    }
}
