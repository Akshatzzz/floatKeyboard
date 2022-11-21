package com.kcdeepak.imeautofill

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.ArraySet
import android.util.AttributeSet
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.inline.InlineContentView
import androidx.annotation.AttrRes
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
class InlineContentClipView(context: Context, attributeSet: AttributeSet, @AttrRes defStyleAttr:Int):
    FrameLayout(context, attributeSet,defStyleAttr) {
    private val clippedDescendants = ArraySet<InlineContentView>()
    private val onDrawListener = this::clipDescendantInlineContentViews
    private val parentBounds = Rect()
    private val contentBounds = Rect()
    private var myBackgroundColor:Int = 0
    private var backgroundView: SurfaceView


    constructor(context: Context):this(context,null)
    constructor(context: Context,attributeSet: AttributeSet?):this(context,attributeSet!!,0)
    
    init {
        backgroundView = SurfaceView(context)
        backgroundView.setZOrderOnTop(true)
        backgroundView.holder.setFormat(PixelFormat.TRANSPARENT)
        backgroundView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        backgroundView.holder.addCallback(object : SurfaceHolder.Callback{
            override fun surfaceCreated(holder: SurfaceHolder) {
                drawBackgroundColorIfReady()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {

            }

        })
        addView(backgroundView)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnDrawListener { onDrawListener }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewTreeObserver.removeOnDrawListener { onDrawListener }
    }

    override fun setBackgroundColor(color:Int){
        myBackgroundColor = color
        Choreographer.getInstance().postFrameCallback {
            drawBackgroundColorIfReady()
        }
    }

    private fun drawBackgroundColorIfReady() {
        val surface = backgroundView.holder.surface
        if(surface.isValid){
            val canvas = surface.lockCanvas(null)
            try{
                canvas.drawColor(myBackgroundColor)
            }finally{
                surface.unlockCanvasAndPost(canvas)
            }
        }
    }

    fun setZOrderedOnTop(onTop:Boolean){
        backgroundView.setZOrderOnTop(onTop)

        for(i in 0 until clippedDescendants.size){
            clippedDescendants.valueAt(i).setZOrderedOnTop(onTop)
        }
    }

    private fun clipDescendantInlineContentViews(){
        parentBounds.right = width
        parentBounds.bottom = height
        clippedDescendants.clear()
        clipDescendantInlineContentViewsRecursive(this)
    }

    private fun clipDescendantInlineContentViewsRecursive(root:View?){
        if(root==null)return
        if(root is InlineContentView) {
            contentBounds.set(parentBounds)
            offsetRectIntoDescendantCoords(root, contentBounds)
            root.clipBounds = contentBounds
            clippedDescendants.add(root)
            return
        }
        if(root is ViewGroup) {
            val childCount = root.childCount
            for (i in 0 until childCount) {
                val child = root.getChildAt(i)
                clipDescendantInlineContentViewsRecursive(child)
            }
        }
    }
}