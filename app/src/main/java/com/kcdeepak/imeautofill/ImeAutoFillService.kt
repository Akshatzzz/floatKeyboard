package com.kcdeepak.imeautofill


import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.Icon
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.roundToInt


@RequiresApi(Build.VERSION_CODES.R)
class ImeAutoFillService : InputMethodService() {
    lateinit var inputView:InputView
    lateinit var keyboard: Keyboard
    lateinit var decoder: Decoder

    lateinit var btn: Button
    lateinit var lin: LinearLayout

    lateinit var suggestionStrip:ViewGroup
    lateinit var pinnedSuggestionsStart:ViewGroup
    lateinit var pinnedSuggestionsEnd:ViewGroup
    lateinit var scrollableSuggestionsClip:InlineContentClipView
    lateinit var scrollableSuggestions:ViewGroup

    var pressed_x:Float? = null
    var pressed_y:Float? = null
    var pressed_x1:Int? = null
    var pressed_y1:Int? = null

    private val handler = Handler(Looper.getMainLooper())
    private var responseState = ResponseState.RESET
    private var delayedDeletion:Runnable?=null
    private var pendingResponse:Runnable? = null

    private val moveScrollableSuggestionsToBg = Runnable {
        scrollableSuggestionsClip.setZOrderedOnTop(false)
        Toast.makeText(this@ImeAutoFillService,"Chips moved to Background-NOT CLICKABLE",Toast.LENGTH_LONG).show()
    }

    private val moveScrollableSuggestionsToFg = Runnable {
        scrollableSuggestionsClip.setZOrderedOnTop(true)
        Toast.makeText(this@ImeAutoFillService,"Chips moved to Foreground-CLICKABLE",Toast.LENGTH_LONG).show()
    }

    private val moveScrollableSuggestionsUp = Runnable {
        suggestionStrip.animate().translationY(-150.0f).setDuration(500).start()
        Toast.makeText(this@ImeAutoFillService,"Animating Up",Toast.LENGTH_LONG).show()
    }

    private val moveScrollableSuggestionsDown = Runnable {
        suggestionStrip.animate().translationY(0f).setDuration(500).start()
        Toast.makeText(this@ImeAutoFillService,"Animating Down",Toast.LENGTH_LONG).show()
    }

    override fun onCreate() {
        super.onCreate()
        inputView = LayoutInflater.from(this).inflate(R.layout.input_view,null) as InputView
        keyboard = Keyboard.qwerty(this)
        inputView.addView(keyboard.inflateKeyboardView(LayoutInflater.from(this),inputView))
        suggestionStrip = inputView.findViewById(R.id.suggestion_strip)
        pinnedSuggestionsStart = inputView.findViewById(R.id.pinned_suggestions_start)
        pinnedSuggestionsEnd = inputView.findViewById(R.id.pinned_suggestions_end)
        scrollableSuggestionsClip = inputView.findViewById(R.id.scrollable_suggestions_clip)
        scrollableSuggestions = inputView.findViewById(R.id.scrollable_suggestions)
        btn = inputView.findViewById(R.id.float_btn)
//        lin = inputView.findViewById(R.id.parent_linearLayout)
    }

    override fun onCreateInputView(): View {
        Log.d(TAG, "onCreateInputView() called")
        return inputView
    }

    override fun onBindInput() {
        super.onBindInput()
        Log.d(TAG, "onBindInput: Service bound to a new client")
    }


    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.d(TAG, "onStartInput() called")
        decoder = Decoder(currentInputConnection)
        if(keyboard!=null){
            keyboard.reset()
        }
        if(responseState == ResponseState.RECEIVE_RESPONSE){
            responseState = ResponseState.START_INPUT
        }
        else{
            responseState = ResponseState.RESET
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        Log.d(TAG, "onFinishInput: ")
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val temp  = keyboard.inflateKeyboardView(LayoutInflater.from(this),inputView)
        btn.setOnTouchListener(mOnTouchListenerTv2)
        Log.d(TAG, "onStartInputView() called")
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        Log.d(TAG, "onFinishInputView: 2")
        if(!finishingInput){
            clearInlineSuggestionStrip()
        }
    }

    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        Log.d(TAG, "onComputeInsets: ")
        if(inputView!=null){
            outInsets?.contentTopInsets = outInsets?.contentTopInsets?.plus(inputView.getTopInsets())
        }
        outInsets?.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest {
        Log.d(TAG, "onCreateInlineSuggestionsRequest() called")
        val stylesBuilder = UiVersions.newStylesBuilder()
        val style = InlineSuggestionUi.newStyleBuilder()
            .setSingleIconChipStyle(
                ViewStyle.Builder()
                    .setBackground(Icon.createWithResource(this,R.drawable.chip_background))
                    .setPadding(0,0,0,0)
                    .build()
            )
            .setChipStyle(
                ViewStyle.Builder()
                    .setBackground(Icon.createWithResource(this,R.drawable.chip_background))
                    .setPadding(toPixel(5f+8f),0,toPixel(5f+8f),0)
                    .build()
            )
            .setStartIconStyle(ImageViewStyle.Builder().setLayoutMargin(0,0,0,0).build())
            .setTitleStyle(
                TextViewStyle.Builder()
                    .setLayoutMargin(0,0,toPixel(4f),0)
                    .setTextColor(Color.parseColor("#FF202124"))
                    .setTextSize(16f)
                    .build()
            )
            .setSubtitleStyle(
                TextViewStyle.Builder()
                    .setLayoutMargin(0,0,toPixel(4f),0)
                    .setTextColor(Color.parseColor("#99202124"))
                    .setTextSize(14f)
                    .build()
            )
            .setEndIconStyle(
                ImageViewStyle.Builder()
                    .setLayoutMargin(0,0,0,0)
                    .build()
            )
            .build()
        stylesBuilder.addStyle(style)
        val stylesBundle = stylesBuilder.build()
        val presentationSpec = ArrayList<InlinePresentationSpec>()
        presentationSpec.add(
            InlinePresentationSpec.Builder(
                Size(100,getHeight()),
                Size(740,getHeight()))
                .setStyle(stylesBundle)
                .build()
        )
        presentationSpec.add(
            InlinePresentationSpec.Builder(
                Size(100,getHeight()),
                Size(740,getHeight()))
                .setStyle(stylesBundle)
                .build()
        )

        return InlineSuggestionsRequest.Builder(presentationSpec)
            .setMaxSuggestionCount(6)
            .build()
    }

    private fun toPixel(dp: Float): Int {
        Log.d(TAG, "toPixel: being called")
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,dp,resources.displayMetrics).roundToInt()
    }

    private fun getHeight():Int{
        return resources.getDimensionPixelSize(R.dimen.keyboard_header_height)
    }

    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        Log.d(TAG, "onInlineSuggestionsResponse: ${response.inlineSuggestions.size}")
        cancelDelayedDeletion("OnInlineSuggestionResponse")
        postPendingResponse(response)
        return true
    }

    private fun cancelPendingResponse(){
        if(pendingResponse!=null){
            Log.d(TAG, "cancelPendingResponse: Cancelling Pending Response")
            handler.removeCallbacks(pendingResponse!!)
            pendingResponse = null
        }
    }

    private fun cancelDelayedDeletion(msg:String){
        if(delayedDeletion!=null){
            Log.d(TAG, "$msg canceling delayed deletion")
            handler.removeCallbacks(delayedDeletion!!)
            delayedDeletion = null
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return true
    }

    private fun postPendingResponse(response:InlineSuggestionsResponse){
        cancelPendingResponse()
        val inlineSuggestions = response.inlineSuggestions
        responseState = ResponseState.RECEIVE_RESPONSE
        pendingResponse = Runnable {
            pendingResponse = null
            if(responseState==ResponseState.START_INPUT && inlineSuggestions.isEmpty()){
                scheduleDelayedDeletion()
            }
            else{
                inflateThenShowSuggestion(inlineSuggestions)
            }
            responseState = ResponseState.RESET
        }
        handler.post(pendingResponse!!)
    }

    private fun inflateThenShowSuggestion(inlineSuggestions:List<InlineSuggestion>) {
        Log.d(TAG, "inflateThenShowSuggestion: ")
        val totalSuggestionCount = inlineSuggestions.size
        if(inlineSuggestions.isEmpty()){
            mainExecutor.execute{
                updateInlineSuggestionStrip(Collections.emptyList())
            }
        }
        val suggestionMap = TreeMap<Int,SuggestionItem?>()
        val executor = Executors.newSingleThreadExecutor()
        for(i in 0 until totalSuggestionCount){
            val index = i
            val inlineSuggestion = inlineSuggestions[i]
            val size = Size(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT)
            inlineSuggestion.inflate(this,size,executor){
                Log.d(TAG, "inflateThenShowSuggestion: New inline suggestion view ready")
                if(it!=null){
                    it.setOnClickListener{
                        Log.d(TAG, "inflateThenShowSuggestion: Received click on suggestion view")
                    }
                    it.setOnLongClickListener{
                        Log.d(TAG, "inflateThenShowSuggestion: Received long click on suggestion view")
                        true
                    }
                    val suggestionView = SuggestionItem(it,inlineSuggestion.info.isPinned)
                    suggestionMap.put(index,suggestionView)
                }
                else{
                    suggestionMap.put(index,null)
                }
                if(suggestionMap.size>=totalSuggestionCount){
                    val suggestionItems:List<SuggestionItem?> = ArrayList(suggestionMap.values)
                    mainExecutor.execute{
                        updateInlineSuggestionStrip(suggestionItems)
                    }
                }
            }
        }
    }

    private fun scheduleDelayedDeletion() {
        if(inputView!=null && delayedDeletion==null){
            Log.d(TAG, "scheduleDelayedDeletion: Scheduling a Delayed deletion of inline suggestion")
            delayedDeletion = Runnable {
                Log.d(TAG, "scheduleDelayedDeletion: Executing scheduled deleting of inline suggestion")
                delayedDeletion = null
                clearInlineSuggestionStrip()
            }
            handler.post(delayedDeletion!!)
        }
    }

    private fun clearInlineSuggestionStrip() {
        if(inputView!=null){
            updateInlineSuggestionStrip(Collections.emptyList())
        }
    }

    private fun updateInlineSuggestionStrip(suggestionItems: List<SuggestionItem?>) {
        Log.d(TAG, "updateInlineSuggestionStrip: Actually updating the suggestion strip : ${suggestionItems.size}")
        pinnedSuggestionsStart.removeAllViews()
        scrollableSuggestions.removeAllViews()
        pinnedSuggestionsEnd.removeAllViews()

        if(suggestionItems.isEmpty()){
            return
        }
        scrollableSuggestionsClip.setBackgroundColor(getColor(R.color.suggestion_strip_background))
        suggestionStrip.visibility = View.VISIBLE

        for(suggestionItem in suggestionItems){
            if(suggestionItem == null){
                continue
            }
            val suggestionView = suggestionItem.view
            if(suggestionItem.isPinned){
                if(pinnedSuggestionsStart.childCount<=0){
                    pinnedSuggestionsStart.addView(suggestionView)
                }
                else{
                    pinnedSuggestionsEnd.addView(suggestionView)
                }
            }else{
                scrollableSuggestions.addView(suggestionView)
            }
        }
        Log.d(TAG, "updateInlineSuggestionStrip: ${SHOWCASE_BG_FG_TRANSITION} 1")
        Log.d(TAG, "updateInlineSuggestionStrip: ${SHOWCASE_UP_DOWN_TRANSITION} 2")
        if(SHOWCASE_BG_FG_TRANSITION){
            rescheduleShowcaseBgFgTransition()
        }
        if(SHOWCASE_UP_DOWN_TRANSITION){
            rescheduleShowcaseUpDownTransition()
        }
    }

    private fun rescheduleShowcaseUpDownTransition() {
        Log.d(TAG, "rescheduleShowcaseUpDownTransition: ")
        val handler = inputView.handler
        handler.removeCallbacks(moveScrollableSuggestionsToBg)
        handler.postDelayed(moveScrollableSuggestionsToBg, MOVE_SUGGESTION_TO_BG_TIMEOUT)
        handler.removeCallbacks(moveScrollableSuggestionsToFg)
        handler.postDelayed(moveScrollableSuggestionsToFg, MOVE_SUGGESTION_TO_FG_TIMEOUT)
    }

    private fun rescheduleShowcaseBgFgTransition() {
        Log.d(TAG, "rescheduleShowcaseBgFgTransition: ")
        val handler = inputView.handler
        handler.removeCallbacks(moveScrollableSuggestionsUp)
        handler.postDelayed(moveScrollableSuggestionsUp, MOVE_SUGGESTION_UP_TIMEOUT)
        handler.removeCallbacks(moveScrollableSuggestionsDown)
        handler.postDelayed(moveScrollableSuggestionsDown, MOVE_SUGGESTION_DOWN_TIMEOUT)
    }

    fun handle(data: String?) {
        Log.d(TAG, "handle: [${data}]")
         decoder.decodeAndApply(data!!)
    }

    companion object{
        const val TAG:String = "ImeAutoFillService"

        const val SHOWCASE_BG_FG_TRANSITION:Boolean = true
        const val SHOWCASE_UP_DOWN_TRANSITION:Boolean = true
        const val MOVE_SUGGESTION_TO_BG_TIMEOUT:Long = 5000
        const val MOVE_SUGGESTION_TO_FG_TIMEOUT:Long = 15000
        const val MOVE_SUGGESTION_UP_TIMEOUT:Long = 5000
        const val MOVE_SUGGESTION_DOWN_TIMEOUT:Long = 15000

        data class SuggestionItem(val view:InlineContentView,val isPinned:Boolean)
    }

    enum class ResponseState{
        RESET,
        RECEIVE_RESPONSE,
        START_INPUT
    }
    private val mOnTouchListenerTv2: View.OnTouchListener? = object: View.OnTouchListener{
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            val relativeLayoutParams = btn.layoutParams as LinearLayout.LayoutParams
            when(event!!.actionMasked){
                MotionEvent.ACTION_DOWN -> {
                    Log.d("Showing","@@@ TV2 ACTION_UP")
                    //where the finger is during the drag
                    pressed_x = event.getRawX()
                    pressed_y = event.getRawY()
                }
                MotionEvent.ACTION_MOVE -> {
                    Log.d("Showing", "tv2 ACTION_MOVE")

                    //Calculate change in x and y
                    val x:Int = event.getRawX().toInt()
                    val y:Int = event.getRawY().toInt()

                    //Update the margins
                    val dx = x - pressed_x!!
                    val dy = y - pressed_y!!

                    //Update the margins
                    relativeLayoutParams.leftMargin += dx.toInt()
                    relativeLayoutParams.topMargin += dy.toInt()
                    btn.layoutParams = relativeLayoutParams

                    //Save where the user's finger was for the next ACTION_MOVE
                    pressed_y = y.toFloat()
                    pressed_x = x.toFloat()

                }
                MotionEvent.ACTION_UP -> {
                    Log.d("Showing", "TV2 ACTION_UP")
                }

            }
            return true
        }
    }
}
