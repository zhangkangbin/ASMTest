package com.test.asm;

import android.support.annotation.Keep;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.test.asm.annotation.XiaoBao;

@RequiresApi
public class TestCode {
    @Keep
    public void test() {
        Log.d("kang", "ams go ....");
    }

    @XiaoBao
    public void remove() {
        Log.d("kang", "ams go ....");
    }
}
