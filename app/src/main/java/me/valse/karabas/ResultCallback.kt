package me.valse.karabas

public class ResultCallback {
    private var returnValue: String? = null

    fun setReturnValue(s: String?) {
        returnValue = s
    }

    fun getReturnValue(): String? {
        return returnValue
    }
}