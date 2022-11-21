package com.kcdeepak.imeautofill

import android.os.Build
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi


@RequiresApi(Build.VERSION_CODES.R)
class Keyboard(imeAutoFillService: ImeAutoFillService,viewResId:Int,keyMapping:SparseArray<String>) {
    private var imeAutoFillService:ImeAutoFillService
    private var keyMapping: SparseArray<String>
    private var viewResId:Int
    private lateinit var keyboardView:View
    private var state:Int
    
    init {
        this.imeAutoFillService = imeAutoFillService
        this.keyMapping = keyMapping
        this.viewResId = viewResId
        this.state = 0
    }

    fun inflateKeyboardView(inflater: LayoutInflater,inputView: InputView):View{
        keyboardView = inflater.inflate(viewResId,inputView,false)
        mapKeys()
        return keyboardView
    }

    private fun mapKeys() {
        for(i in 0 until keyMapping.size()){
            val softKey:TextView = keyboardView.findViewById(keyMapping.keyAt(i))
            val rawData = keyMapping.valueAt(i)
            val data = if(rawData.length!= NUM_STATES){
                rawData
            }else{
                rawData.substring(state,state+1)
            }
            softKey.text = getLabel(data)
            softKey.setOnClickListener {
                handle(data)
            }
        }
    }


    private fun handle(data: String?) {
        when(data){
            "SHI"->{
                state = state xor STATE_SHIFT
                mapKeys()
            }
            "SYM"->{
                state = state xor STATE_SYMBOL
                mapKeys()
            }
            else->{
                imeAutoFillService.handle(data)
            }
        }
    }

    fun reset() {
        state = 0
        mapKeys()
    }

    companion object{
        const val NUM_STATES:Int = 4
        const val STATE_SHIFT:Int = 1
        const val STATE_SYMBOL:Int = 2

        fun getLabel(data:String):String{
            return when(data){
                "SHI"->{
                    "↑"
                }
                "DEL"->{
                    "←"
                }
                "SYM"->{
                    "?123"
                }
                "SPA"->{
                    "               "
                }
                "ENT"->{
                    "↩"
                }
                else->{
                    data
                }
            }
        }
        fun qwerty(imeAutoFillService: ImeAutoFillService):Keyboard{
            val keyMapping = SparseArray<String>()
            keyMapping.put(R.id.key_pos_0_0, "qQ1\u007E");
            keyMapping.put(R.id.key_pos_0_1, "wW2\u0060");
            keyMapping.put(R.id.key_pos_0_2, "eE3\u007C");
            keyMapping.put(R.id.key_pos_0_3, "rR4\u2022");
            keyMapping.put(R.id.key_pos_0_4, "tT5\u221A");
            keyMapping.put(R.id.key_pos_0_5, "yY6\u03C0");
            keyMapping.put(R.id.key_pos_0_6, "uU7\u00F7");
            keyMapping.put(R.id.key_pos_0_7, "iI8\u00D7");
            keyMapping.put(R.id.key_pos_0_8, "oO9\u00B6");
            keyMapping.put(R.id.key_pos_0_9, "pP0\u2206");
            keyMapping.put(R.id.key_pos_1_0, "aA@\u00A3");
            keyMapping.put(R.id.key_pos_1_1, "sS#\u00A2");
            keyMapping.put(R.id.key_pos_1_2, "dD$\u20AC");
            keyMapping.put(R.id.key_pos_1_3, "fF_\u00A5");
            keyMapping.put(R.id.key_pos_1_4, "gG&\u005E");
            keyMapping.put(R.id.key_pos_1_5, "hH-=");
            keyMapping.put(R.id.key_pos_1_6, "jJ+{");
            keyMapping.put(R.id.key_pos_1_7, "kK(}");
            keyMapping.put(R.id.key_pos_1_8, "lL)\\");
            keyMapping.put(R.id.key_pos_2_0, "zZ*%");
            keyMapping.put(R.id.key_pos_2_1, "xX\"\u00A9");
            keyMapping.put(R.id.key_pos_2_2, "cC'\u00AE");
            keyMapping.put(R.id.key_pos_2_3, "vV:\u2122");
            keyMapping.put(R.id.key_pos_2_4, "bB;\u2713");
            keyMapping.put(R.id.key_pos_2_5, "nN![");
            keyMapping.put(R.id.key_pos_2_6, "mM?]");
            keyMapping.put(R.id.key_pos_bottom_0, ",,,<");
            keyMapping.put(R.id.key_pos_bottom_1, "...>");
            keyMapping.put(R.id.key_pos_shift, "SHI");
            keyMapping.put(R.id.key_pos_del, "DEL");
            keyMapping.put(R.id.key_pos_symbol, "SYM");
            keyMapping.put(R.id.key_pos_space, "SPA");
            keyMapping.put(R.id.key_pos_enter, "ENT");

            return Keyboard(imeAutoFillService,R.layout.keyboard,keyMapping)
        }
    }
}