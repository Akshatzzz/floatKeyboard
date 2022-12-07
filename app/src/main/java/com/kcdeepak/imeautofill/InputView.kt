package com.kcdeepak.imeautofill

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout

class InputView(context: Context, attributeSet: AttributeSet) : LinearLayout(context,attributeSet) {
    var realHeight:Int=0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        realHeight = measuredHeight
        if(EXPAND_TO_WINDOW && MeasureSpec.getMode(heightMeasureSpec)==MeasureSpec.AT_MOST){
            setMeasuredDimension(measuredWidth,MeasureSpec.getSize(heightMeasureSpec))
        }
    }

    fun getTopInsets():Int{
        return measuredHeight-realHeight
    }

    companion object{
        private const val EXPAND_TO_WINDOW:Boolean = true
    }
}