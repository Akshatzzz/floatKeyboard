package com.kcdeepak.imeautofill

import android.view.KeyEvent
import android.view.inputmethod.InputConnection


class Decoder(inputConnection: InputConnection) {
    var inputConnection: InputConnection

    init {
        this.inputConnection = inputConnection
    }

    fun decodeAndApply(data:String){
        when(data){
            "DEL"->{
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            }
            "ENT"->{
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER))
            }
            "SPA"->{
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_SPACE))
            }
            else->{
                inputConnection.commitText(data,1)
            }
        }
    }

    fun isEmpty():Boolean{
        return (inputConnection.getTextBeforeCursor(1, 0)!!.isEmpty() && inputConnection.getTextAfterCursor(1,0)!!.isEmpty())
    }
}