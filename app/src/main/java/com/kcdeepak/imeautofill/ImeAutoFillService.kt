package com.kcdeepak.imeautofill


import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Icon
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InlineSuggestionsRequest
import android.widget.*
import android.widget.inline.InlineContentView
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.common.ImageViewStyle
import androidx.autofill.inline.common.TextViewStyle
import androidx.autofill.inline.common.ViewStyle
import androidx.autofill.inline.v1.InlineSuggestionUi
import androidx.constraintlayout.widget.ConstraintLayout
import java.util.*
import kotlin.math.roundToInt


@RequiresApi(Build.VERSION_CODES.R)
class ImeAutoFillService : InputMethodService() {
    lateinit var inputView: InputView
    lateinit var keyboard: Keyboard
    lateinit var decoder: Decoder

    lateinit var fBtn: Button
    lateinit var mBtn: Button
    lateinit var lin: LinearLayout
    lateinit var constraint: ConstraintLayout

    lateinit var kTopRow: LinearLayout
    lateinit var kRow1: LinearLayout
    lateinit var kRow2: LinearLayout
    lateinit var kRow3: LinearLayout
    lateinit var kRow4: LinearLayout

    lateinit var kHandle: Button

//    lateinit var suggestionStrip: ViewGroup
//    lateinit var pinnedSuggestionsStart: ViewGroup
//    lateinit var pinnedSuggestionsEnd: ViewGroup
//    lateinit var scrollableSuggestionsClip: InlineContentClipView
//    lateinit var scrollableSuggestions: ViewGroup

    var pressed_x: Float? = null
    var pressed_y: Float? = null
    var pressed_x1: Int? = null
    var pressed_y1: Int? = null

    var rightDY: Float = 0.0f
    var rightDX: Float = 0.0f

    var flag:Int = 1
    var temp: Float = 0.0f

    var temp1:Double = 0.0
    var temp2:Double = 0.0
    var temp3:Float = 0f
    var temp4:Float = 0f

    var newScale: Double =1.0

    private val handler = Handler(Looper.getMainLooper())
    private var responseState = ResponseState.RESET
    private var delayedDeletion: Runnable? = null
    private var pendingResponse: Runnable? = null


//    private val moveScrollableSuggestionsToBg = Runnable {
//        scrollableSuggestionsClip.setZOrderedOnTop(false)
//        Toast.makeText(
//            this@ImeAutoFillService,
//            "Chips moved to Background-NOT CLICKABLE",
//            Toast.LENGTH_LONG
//        ).show()
//    }
//
//    private val moveScrollableSuggestionsToFg = Runnable {
//        scrollableSuggestionsClip.setZOrderedOnTop(true)
//        Toast.makeText(
//            this@ImeAutoFillService,
//            "Chips moved to Foreground-CLICKABLE",
//            Toast.LENGTH_LONG
//        ).show()
//    }
//
//    private val moveScrollableSuggestionsUp = Runnable {
//        suggestionStrip.animate().translationY(-150.0f).setDuration(500).start()
//        Toast.makeText(this@ImeAutoFillService, "Animating Up", Toast.LENGTH_LONG).show()
//    }

//    private val moveScrollableSuggestionsDown = Runnable {
//        suggestionStrip.animate().translationY(0f).setDuration(500).start()
//        Toast.makeText(this@ImeAutoFillService,"Animating Down",Toast.LENGTH_LONG).show()
//    }

    override fun onCreate() {
        super.onCreate()
        inputView = LayoutInflater.from(this).inflate(R.layout.input_view, null) as InputView
        keyboard = Keyboard.qwerty(this)
        inputView.addView(keyboard.inflateKeyboardView(LayoutInflater.from(this), inputView))
//        suggestionStrip = inputView.findViewById(R.id.suggestion_strip)
//        pinnedSuggestionsStart = inputView.findViewById(R.id.pinned_suggestions_start)
//        pinnedSuggestionsEnd = inputView.findViewById(R.id.pinned_suggestions_end)
//        scrollableSuggestionsClip = inputView.findViewById(R.id.scrollable_suggestions_clip)
//        scrollableSuggestions = inputView.findViewById(R.id.scrollable_suggestions)
        fBtn = inputView.findViewById(R.id.float_btn)
        mBtn = inputView.findViewById(R.id.move_btn)

//        constraint = inputView.findViewById(R.id.parent_constraint)
        lin = inputView.findViewById(R.id.parent_layout)
        kTopRow = inputView.findViewById(R.id.topRow)
        kRow1 = inputView.findViewById(R.id.row1)
        kRow2 = inputView.findViewById(R.id.row2)
        kRow3 = inputView.findViewById(R.id.row3)
        kRow4 = inputView.findViewById(R.id.row4)

        kHandle = inputView.findViewById(R.id.handle)

    }

    override fun onCreateInputView(): View {
//        Log.d(TAG, "onCreateInputView() called")
        return inputView
    }

    override fun onBindInput() {
        super.onBindInput()
//        Log.d(TAG, "onBindInput: Service bound to a new client")
    }


    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
//        Log.d(TAG, "onStartInput() called")
        decoder = Decoder(currentInputConnection)
        if (keyboard != null) {
            keyboard.reset()
        }
        if (responseState == ResponseState.RECEIVE_RESPONSE) {
            responseState = ResponseState.START_INPUT
        } else {
            responseState = ResponseState.RESET
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()
//        Log.d(TAG, "onFinishInput: ")
    }

    //add below annotation to remove the yello text override performClick warning
    @SuppressLint("ClickableViewAccessibility")
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val temp = keyboard.inflateKeyboardView(LayoutInflater.from(this), inputView)
        if (flag == 1) {
            mBtn.setOnTouchListener(mOnTouchListenerTv2)
//            lin.x= ((inputView.width-lin.width)/2).toFloat()
//            kHandle.setVisibility(View.VISIBLE)
        }


        kHandle.setOnTouchListener(
        object : View.OnTouchListener {
            var centerX = 0f
            var centerY = 0f
            var startR = 0f
            var startX = 0f
            var startY = 0f
            var startScale = 0f

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN) {
//                    Log.d("****",
//                        "${button.getLeft()},   ${button.x},   ${button.getRight()},  ${button.width},  ${parentLayout.width} ")
//                    Log.d("****", "${(button.getLeft() + button.getRight()) / 2f}")

                    // calculate center of image
                    centerX = (lin.getLeft() + lin.getRight()) / 2f
                    centerY = (lin.getTop() + lin.getBottom()) / 2f

//                    Log.d("****", "${event.getRawX()},  ${dragHandle.getX()},    ${centerX}")
                    // recalculate coordinates of starting point
                    Log.d("&&&&", "${kHandle.getY()},  ${centerY}")

                    /* below lin.getX() and lin.getY() is added to fix inverse resizing  (we want coordinates with respect
                    to whole screen, instead of parent layout(which is not whole scree in this case)*/

                    /* (kHandle.getX()+lin.getX()) kHnadle ke top right coordinates as it moves in inputView */
//                    startX = event.getRawX() - (kHandle.getX()+lin.getX()) + centerX;
//                    startY = event.getRawY() - (kHandle.getY()+lin.getY()) + centerY;

                    startX=lin.getX()
                    startY=lin.getY()
                    Log.d("@@@", "${event.getRawX()}  ${kHandle.getX()} ${lin.getX()}  $centerX")
                    Log.d("@@@", "${event.getRawY()}  ${kHandle.getY()} ${lin.getY()}  $centerY")

//                    Log.d("&&&&", "${startX}")

                    // get starting distance and scale
                    startR = Math.hypot((event.getRawX() - startX).toDouble(),
                        (event.getRawY() - startY).toDouble()).toFloat()
                    startScale = lin.getScaleX()

//                    Log.d("****",
//                        "${(event.getRawX() - startX)},  startR: ${startR},  startScale: ${startScale}")

                } else if (event?.action == MotionEvent.ACTION_MOVE) {

                    // calculate new distance
                    Log.d("&&&&", "startX: ${startX}, eventX: ${event.getRawX()}, startY:${startY}, eventY: ${event.getRawY()}")

                    var newR: Double = Math.hypot((event.getRawX() - startX).toDouble(),
                        (event.getRawY() - startY).toDouble())

                    //set new scale
                    newScale = newR / startR * startScale

                    //temp1, temp2 for adjusting bounds after resizing
                    //no need to calcuate them on moving lin, when scale remains same in that condition
                    temp1 =(lin.width - lin.width*newScale)/2
                    temp2 =(lin.height - lin.height*newScale)/2

                    Log.d("&&&&", "newR: ${newR}, newScale: ${newScale}")

                    //to not resize from centre
//                    lin.setPivotX(50f)
//                    lin.setPivotY(50f)

                    //working in mysterious ways
//                        lin.setPivotY(300f)


                    //same as below
                    lin.animate().scaleX(newScale.toFloat())
                        .scaleY(newScale.toFloat())
                        .setDuration(0).start()
//                    lin.setScaleX(newScale.toFloat())
//                    lin.setScaleY(newScale.toFloat())



//                    //move handler, NO NEED HERE as we are animating the whole view which includes the handle
//                    kHandle.setX(((centerX) + lin.getWidth() / 2f * newScale).toFloat())
//                    kHandle.setY(((centerY) + lin.getHeight() / 2f * newScale).toFloat())


                } else if (event?.getAction() == MotionEvent.ACTION_UP) {
                }

                return true
            }

        })

//        Log.d("******", "onStartInputView() called")
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
//        Log.d(TAG, "onFinishInputView: 2")
//        if (!finishingInput) {
//            clearInlineSuggestionStrip()
//        }
    }

    override fun onComputeInsets(outInsets: Insets?) {

        if (flag == 1) {
            fBtn.setOnClickListener{
                flag=0
            }


            //earlier 800 as width
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT

            ).apply {
                gravity = Gravity.CENTER_VERTICAL

                //after installing, first time onpening keyboard, this value is 0
                leftMargin = (inputView.width - lin.width) / 2

//                Log.d("*****", "${(inputView.width-lin.width)/2}")
            }
            lin.layoutParams = params
            kHandle.setVisibility(View.VISIBLE)
            super.onComputeInsets(outInsets)
//            Log.d(TAG, "onComputeInsets: ")
            if (inputView != null) {
                outInsets?.contentTopInsets =
                    (outInsets?.contentTopInsets)?.plus(inputView.getTopInsets())?.plus(2200)
            }
            val region = Region()
            val keyboardWidth = inputView.width
            val keyboardHeight = inputView.height
//        val location: IntArray = IntArray(2)
//        btn.getLocationOnScreen(location)

//            Log.d("^^^", "temp1: ${temp1}, temp2: ${temp2}")

            region.union(Rect((lin.x.toInt() + temp1).toInt(),
                (lin.y.toInt() + temp2 ).toInt(),
                (lin.x.toInt() + lin.width -temp1).toInt(),
                (lin.y.toInt() + lin.height - temp2 ).toInt()))
            outInsets?.touchableRegion?.set(region)
            outInsets?.touchableInsets = Insets.TOUCHABLE_INSETS_REGION

//        Log.d("MyCoordinates","X: ${location[0].toString()},y: ${location[1].toString()}")
        } else if (flag == 0) {

            kHandle.setVisibility(View.GONE)

            lin.setScaleX(1.toFloat())
            lin.setScaleY(1.toFloat())

            fBtn.setOnClickListener{
                flag=1

//                //so that touchable region works fine after toggling float mode from docked mode
//                //won't work cause temp1 and temp2 are calculated while scaling and not on compute insets
//                newScale= 1.0

                //so that touchable region works fine after toggling float mode from docked mode
                temp1= 0.0
                temp2= 0.0
            }
//            setMargins(inputView,0,0,0,0)
            lin.animate().x(0f).y((inputView.height - lin.height).toFloat()).setDuration(0).start()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1.0f
                gravity = Gravity.BOTTOM
            }
            lin.layoutParams = params
//            setMargins(kTopRow,0,0,0,0)
            super.onComputeInsets(outInsets)
//            Log.d(TAG, "onComputeInsets: ")
            if (inputView != null) {
                outInsets?.contentTopInsets =
                    outInsets?.contentTopInsets?.plus(inputView.getTopInsets())
            }
            outInsets?.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
        }
    }

    fun setMargins(v: View, left: Int, top: Int, right: Int, bottom: Int) {
        if (v.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = v.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            v.requestLayout()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest {
//        Log.d(TAG, "onCreateInlineSuggestionsRequest() called")
        val stylesBuilder = UiVersions.newStylesBuilder()
        val style = InlineSuggestionUi.newStyleBuilder()
            .setSingleIconChipStyle(
                ViewStyle.Builder()
                    .setBackground(Icon.createWithResource(this, R.drawable.chip_background))
                    .setPadding(0, 0, 0, 0)
                    .build()
            )
            .setChipStyle(
                ViewStyle.Builder()
                    .setBackground(Icon.createWithResource(this, R.drawable.chip_background))
                    .setPadding(toPixel(5f + 8f), 0, toPixel(5f + 8f), 0)
                    .build()
            )
            .setStartIconStyle(ImageViewStyle.Builder().setLayoutMargin(0, 0, 0, 0).build())
            .setTitleStyle(
                TextViewStyle.Builder()
                    .setLayoutMargin(0, 0, toPixel(4f), 0)
                    .setTextColor(Color.parseColor("#FF202124"))
                    .setTextSize(16f)
                    .build()
            )
            .setSubtitleStyle(
                TextViewStyle.Builder()
                    .setLayoutMargin(0, 0, toPixel(4f), 0)
                    .setTextColor(Color.parseColor("#99202124"))
                    .setTextSize(14f)
                    .build()
            )
            .setEndIconStyle(
                ImageViewStyle.Builder()
                    .setLayoutMargin(0, 0, 0, 0)
                    .build()
            )
            .build()
        stylesBuilder.addStyle(style)
        val stylesBundle = stylesBuilder.build()
        val presentationSpec = ArrayList<InlinePresentationSpec>()
        presentationSpec.add(
            InlinePresentationSpec.Builder(
                Size(100, getHeight()),
                Size(740, getHeight())
            )
                .setStyle(stylesBundle)
                .build()
        )
        presentationSpec.add(
            InlinePresentationSpec.Builder(
                Size(100, getHeight()),
                Size(740, getHeight())
            )
                .setStyle(stylesBundle)
                .build()
        )

        return InlineSuggestionsRequest.Builder(presentationSpec)
            .setMaxSuggestionCount(6)
            .build()
    }

    private fun toPixel(dp: Float): Int {
//        Log.d(TAG, "toPixel: being called")
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
            .roundToInt()
    }

    private fun getHeight(): Int {
        return resources.getDimensionPixelSize(R.dimen.keyboard_header_height)
    }

//    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
//        Log.d(TAG, "onInlineSuggestionsResponse: ${response.inlineSuggestions.size}")
//        cancelDelayedDeletion("OnInlineSuggestionResponse")
//        postPendingResponse(response)
//        return true
//    }

    private fun cancelPendingResponse() {
        if (pendingResponse != null) {
//            Log.d(TAG, "cancelPendingResponse: Cancelling Pending Response")
            handler.removeCallbacks(pendingResponse!!)
            pendingResponse = null
        }
    }

    private fun cancelDelayedDeletion(msg: String) {
        if (delayedDeletion != null) {
//            Log.d(TAG, "$msg canceling delayed deletion")
            handler.removeCallbacks(delayedDeletion!!)
            delayedDeletion = null
        }
    }

    fun handle(data: String?) {
//        Log.d(TAG, "handle: [${data}]")
        decoder.decodeAndApply(data!!)
    }

    companion object {
        const val TAG: String = "ImeAutoFillService"
//        var flag = 1
        const val SHOWCASE_BG_FG_TRANSITION: Boolean = true
        const val SHOWCASE_UP_DOWN_TRANSITION: Boolean = true
        const val MOVE_SUGGESTION_TO_BG_TIMEOUT: Long = 5000
        const val MOVE_SUGGESTION_TO_FG_TIMEOUT: Long = 15000
        const val MOVE_SUGGESTION_UP_TIMEOUT: Long = 5000
        const val MOVE_SUGGESTION_DOWN_TIMEOUT: Long = 15000

        data class SuggestionItem(val view: InlineContentView, val isPinned: Boolean)
    }

    enum class ResponseState {
        RESET,
        RECEIVE_RESPONSE,
        START_INPUT
    }

    private val mOnTouchListenerTv2: View.OnTouchListener? = object : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (event!!.actionMasked) {
                MotionEvent.ACTION_DOWN -> {

                    //view.getY() gives the relative y coordinates of the view wrt to the parent view
                    rightDY = lin!!.getY() - event.rawY;
                    rightDX = lin!!.getX() - event.rawX

                    Log.d("****",
                        "downERawY: ${event.rawY}, y: ${lin!!.getY()}, rightDY: ${rightDY}")

                    temp = lin!!.getX()
                }
                MotionEvent.ACTION_MOVE -> {

                    if (flag == 1) {
                        lin.setAlpha(0.7f)
                    }

                    var yDisplacement = event.rawY + rightDY
                    var xDisplacement = event.rawX + rightDX

                    Log.d("****", "moveERawY: ${event.rawY}, yDisplacement: ${yDisplacement}")

                    //top bound
                    if (xDisplacement < 0 - temp1) {
                        xDisplacement = (0 - temp1).toFloat()
                    }

                    //right bound
                    if (xDisplacement > inputView.width - (lin.width - temp1)) {
                        xDisplacement = (inputView.width - (lin.width-temp1)).toFloat()
                    }

                    //left bound
                    if (yDisplacement < 0 - temp2) {
                        yDisplacement = (0 - temp2).toFloat()
                    }

                    //this flag==0 prevents keyboard from moving in docked mode, KEEP IT!
                    if (yDisplacement > inputView.height - (lin.height-temp2) || flag == 0) {

                        kHandle.setVisibility(View.GONE)
//                        yDisplacement= (inputView.height-lin.height).toFloat()

                        //if not then dock krne par keyboard (jo static hai) neeche toh jaayega but apne animate vaale view ke saath (toh mtlb aur bottom dikhai dega)
                        lin!!.animate()
                            .x(0F)
                            .y((inputView.height - lin.height).toFloat())
                            .setDuration(0)
                            .start()

                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            weight = 1.0f
                            gravity = Gravity.BOTTOM
                        }
                        lin.layoutParams = params
//                                lin.getLayoutParams().height = 593

//                        kHandle.setVisibility(View.GONE)
                        flag = 0
//                                setMargins(kTopRow,0,0,0,0)
//                                Log.d("$$$$", "flag is: ${flag}")
//                                setMargins(lin,0,inputView.height -(kTopRow.height + kRow1.height + kRow2.height + kRow3.height + kRow4.height),0,0 )

                        return true

                    }


//                    if ((yDisplacement >= inputView.height - (kTopRow.height + kRow1.height + kRow2.height + kRow3.height + kRow4.height)) || flag == 0) {
//                        val params = LinearLayout.LayoutParams(
//                            LinearLayout.LayoutParams.WRAP_CONTENT,
//                            LinearLayout.LayoutParams.WRAP_CONTENT
//                        ).apply {
//                            weight = 1.0f
//                            gravity = Gravity.BOTTOM
//                        }
//                        lin.layoutParams = params
////                                lin.getLayoutParams().height = 593
//
////                        kHandle.setVisibility(View.GONE)
//                        flag = 0
////                                setMargins(kTopRow,0,0,0,0)
////                                Log.d("$$$$", "flag is: ${flag}")
////                                setMargins(lin,0,inputView.height -(kTopRow.height + kRow1.height + kRow2.height + kRow3.height + kRow4.height),0,0 )
//
//                        return true
//                    }

//                    //my bound code below (produces jank)
//                    if(xDisplacement<=0 || xDisplacement>=(inputView.width - lin.width)) {
//                        return true
//                    }


                    lin!!.animate()
                        .x(xDisplacement)
                        .y(yDisplacement)
                        .setDuration(0)
                        .start()

                    temp3=xDisplacement
                    temp4=yDisplacement


                }
                MotionEvent.ACTION_UP -> {
                    lin.setAlpha(1.0f)
                    return true
                }

            }
            return true
        }
    }
}

