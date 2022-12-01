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
import android.view.inputmethod.InlineSuggestion
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
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
import androidx.core.view.get
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
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

    lateinit var kHandle:Button

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

    var temp:Float=0.0f

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

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val temp = keyboard.inflateKeyboardView(LayoutInflater.from(this), inputView)
        if (flag == 1) {
            mBtn.setOnTouchListener(mOnTouchListenerTv2)
            val params = LinearLayout.LayoutParams(
                800, LinearLayout.LayoutParams.WRAP_CONTENT

            ).apply {
                gravity = Gravity.CENTER_VERTICAL

                //after installing, first time onpening keyboard, this value is 0
                leftMargin=(inputView.width-lin.width)/2

                Log.d("*****", "${(inputView.width-lin.width)/2}")
            }
            lin.layoutParams = params
//            lin.x= ((inputView.width-lin.width)/2).toFloat()
            kHandle.setVisibility(View.VISIBLE)
        }

        kHandle.setOnTouchListener(object : View.OnTouchListener {
            var centerX = 0f
            var centerY = 0f
            var startR = 0f
            var startX = 0f
            var startY = 0f
            var startScale = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN) {
//                    Log.d("****",
//                        "${button.getLeft()},   ${button.x},   ${button.getRight()},  ${button.width},  ${parentLayout.width} ")
//                    Log.d("****", "${(button.getLeft() + button.getRight()) / 2f}")

                    // calculate center of image
                    centerX = (lin.getLeft() + lin.getRight()) / 2f
                    centerY = (lin.getTop() + lin.getBottom()) / 2f;

//                    Log.d("****", "${event.getRawX()},  ${dragHandle.getX()},    ${centerX}")
                    // recalculate coordinates of starting point
                    startX = event.getRawX() - kHandle.getX() + centerX;
                    startY = event.getRawY() - kHandle.getY() + centerY;

                    Log.d("****", "${startX}")

                    // get starting distance and scale
                    startR = Math.hypot((event.getRawX() - startX).toDouble(),
                        (event.getRawY() - startY).toDouble()).toFloat()
                    startScale = lin.getScaleX()

                    Log.d("****",
                        "${(event.getRawX() - startX)},  startR: ${startR},  startScale: ${startScale}")

                } else if (event?.action == MotionEvent.ACTION_MOVE) {

//                    if (dragHandle.getX() + dragHandle.width +10 >= parentLayout.width) {
//                        return true
//                    }

                    // calculate new distance
                    var newR: Double = Math.hypot((event.getRawX() - startX).toDouble(),
                        (event.getRawY() - startY).toDouble())

                    //set new scale
                    var newScale = newR / startR * startScale


//                    Log.d("****", "newR: ${newR},    scale: ${newScale}")

//                    Log.d("****",
//                        " ${dragHandle.width+dragHandle.getX()},  dragHandle.getX(): ${dragHandle.getX()}, parentLayout.width: ${parentLayout.width} ")

                    //setting constraints
//                    if (newScale <0.5) {
//                        newScale =0.5
//                    }
//                    if(newScale>=1.5) {
//                        newScale -= (newScale-1.5)
//                    }

//                    if(newScale>1.5) {
//                        newScale=1.5
//                    }

//                    Log.d("****", "x: ${dragHandle.getX()},  width: ${dragHandle.width}")


//                    if(dragHandle.getX() + dragHandle.width >= parentLayout.width) {
//                        dragHandle.setX(dragHandle.getX()-7*dragHandle.width)
//                    }
                    lin.setScaleX(newScale.toFloat())
                    lin.setScaleY(newScale.toFloat())


                    //move handler
                    kHandle.setX((centerX + lin.getWidth() / 2f * newScale).toFloat())
                    kHandle.setY((centerY + lin.getHeight() / 2f * newScale).toFloat())


                } else if (event?.getAction() == MotionEvent.ACTION_UP) {
                }

                return true
            }
        })

//        btn.setOnTouchListener(View.OnTouchListener { view, event ->
//            val relativeLayoutParams = btn.layoutParams as LinearLayout.LayoutParams
//            when (event?.action) {
//                MotionEvent.ACTION_DOWN -> {
//
////               rightDX = view!!.x - event.rawX
//                    rightDY = view!!.getY() - event.rawY;
//                    Log.d("&&&&", "$rightDY .... ${view!!.getY()} .... ${event.rawY} ... ${lin.height}....${inputView.height}")
//
//                    pressed_y = event.getRawY()
//
//                }
//                MotionEvent.ACTION_MOVE -> {
//
//                    var yDisplacement = event.rawY + rightDY
//
////                    if(yDisplacement<=0 || yDisplacement>=parentView.height - floatView.height) {
////                        return@OnTouchListener true
////                    }
//
//                    val y:Int = event.getRawY().toInt()
//                    val dy = y - pressed_y!!
//
//                    lin!!.animate()
////                        .x(displacement)
//                        .y(yDisplacement)
//                        .setDuration(0)
//                        .start()
//
//                    relativeLayoutParams.topMargin += dy.toInt()
//                    btn.layoutParams = relativeLayoutParams
//                    pressed_y = y.toFloat()
//                }
//                else -> { // Note the block
//                    return@OnTouchListener false
//                }
//            }
//            true
//        })
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
//            fBtn.setOnClickListener{
//                flag=0
//            }
//            val params = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.MATCH_PARENT
//            ).apply {
//                weight = 1.0f
//                gravity = Gravity.CENTER
//            }
//            inputView.layoutParams =params
//            setMargins(lin, 20, 0, 20,0)
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

            region.union(Rect(lin.x.toInt(),
                lin.y.toInt(),
                lin.x.toInt() + lin.width,
                lin.y.toInt() + lin.height))
            outInsets?.touchableRegion?.set(region)
            outInsets?.touchableInsets = Insets.TOUCHABLE_INSETS_REGION

//        Log.d("MyCoordinates","X: ${location[0].toString()},y: ${location[1].toString()}")
        } else if (flag == 0) {

//            fBtn.setOnClickListener{
//                flag=1
//            }
//            setMargins(inputView,0,0,0,0)
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
        var flag = 1
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
            val relativeLayoutParams = lin.layoutParams as LinearLayout.LayoutParams
            when (event!!.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
//                    Log.d("@@@@", "flag is: ${flag}")
                    //where the finger is during the drag
                    pressed_x = event.getRawX()
                    pressed_y = event.getRawY()

                    //view.getY() gives the relative y coordinates of the view wrt to the parent view
                    rightDY = lin!!.getY() - event.rawY;
                    rightDX = lin!!.getX() - event.rawX

                    temp=lin!!.getX()
                }
                MotionEvent.ACTION_MOVE -> {
//                    Log.d("&&&&", "flag is: ${flag}")

//                    if(lin!=null) {
//                        lin.background.alpha = 100
//                    }

                    if(flag==1) {
                        lin.setAlpha(0.7f)
                    }
                    var yDisplacement = event.rawY + rightDY
                    var xDisplacement = event.rawX + rightDX

//                    yDisplacement<=0 ||

                    if ((yDisplacement >= inputView.height - (kTopRow.height + kRow1.height + kRow2.height + kRow3.height + kRow4.height)) || flag == 0) {
//                                lin.y= 1680F
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            weight = 1.0f
                            gravity = Gravity.BOTTOM
                        }
                        lin.layoutParams = params
//                                lin.getLayoutParams().height = 593

                        kHandle.setVisibility(View.GONE)
                        flag = 0
//                                setMargins(kTopRow,0,0,0,0)
//                                Log.d("$$$$", "flag is: ${flag}")
//                                setMargins(lin,0,inputView.height -(kTopRow.height + kRow1.height + kRow2.height + kRow3.height + kRow4.height),0,0 )

                        return true
                    }

//                    //my bound code below (produces jank)
//                    if(xDisplacement<=0 || xDisplacement>=(inputView.width - lin.width)) {
//                        return true
//                    }

                    //Calculate change in x and y
                    val x: Int = event.getRawX().toInt()
                    val y: Int = event.getRawY().toInt()

                    //Update the margins
                    var dx = x - pressed_x!!
                    val dy = y - pressed_y!!

//                    dx = dx + temp
//
//                    if(dx<0) {
//                        dx=0f
//                    }
//
//                    dx=dx-temp

//                    Update the margins


                    if(xDisplacement<0) {
                        xDisplacement= 0F
                    }

                    lin!!.animate()
                        .x(xDisplacement)
                        .y(yDisplacement)
                        .setDuration(0)
                        .start()

//                    relativeLayoutParams.leftMargin += dx.toInt()
//                    relativeLayoutParams.topMargin += dy.toInt()
//                    lin.layoutParams = relativeLayoutParams

                    //Save where the user's finger was for the next ACTION_MOVE
                    pressed_y = y.toFloat()
                    pressed_x = x.toFloat()


                }
                MotionEvent.ACTION_UP -> {
//                    if(lin!=null) {
//                        lin.background.alpha = 255
//                    }
                    lin.setAlpha(1.0f)
                    return true
                }

            }
            return true
        }
    }
}

