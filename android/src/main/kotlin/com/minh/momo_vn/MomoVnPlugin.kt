
package com.minh.momo_vn

import android.app.Activity
import android.content.Intent
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.content.pm.PackageManager
import androidx.annotation.NonNull
import vn.momo.momo_partner.AppMoMoLib

class MomoVnPlugin : MethodCallHandler,FlutterPlugin,ActivityAware, PluginRegistry.ActivityResultListener {
  //  private val momoVnPluginDelegate = MomoVnPluginDelegate(registrar)
    private lateinit var channel : MethodChannel
    var act: Activity? = null
//    init {
//        registrar.addActivityResultListener(momoVnPluginDelegate)
//    }
    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
      //  val momoPaymentPlugin = MomoVnPlugin(registrar)
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, MomoVnConfig.CHANNEL_NAME)
        channel.setMethodCallHandler(this)
    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            MomoVnConfig.METHOD_REQUEST_PAYMENT -> {
                this.openCheckout(call.arguments, result)
            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        act = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        act = null;
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        act = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        act = null;
    }


    private var pendingResult: Result? = null
    private var pendingReply: Map<String, Any>? = null

    fun openCheckout(momoRequestPaymentData: Any, result: Result) {
        this.pendingResult = result;
        AppMoMoLib.getInstance().setAction(AppMoMoLib.ACTION.PAYMENT)
        AppMoMoLib.getInstance().setActionType(AppMoMoLib.ACTION_TYPE.GET_TOKEN)

        val paymentInfo: HashMap<String, Any> = momoRequestPaymentData as HashMap<String, Any>
        val isTestMode: Boolean? = paymentInfo["isTestMode"] as Boolean?

        if (isTestMode == null || !isTestMode) {
            AppMoMoLib.getInstance().setEnvironment(AppMoMoLib.ENVIRONMENT.PRODUCTION)
        } else {
            AppMoMoLib.getInstance().setEnvironment(AppMoMoLib.ENVIRONMENT.DEVELOPMENT)
        }

        AppMoMoLib.getInstance().requestMoMoCallBack(act, paymentInfo)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AppMoMoLib.getInstance().REQUEST_CODE_MOMO) {
                _handleResult(data)
            }
        }
        return true
    }
    private fun sendReply(data: Map<String, Any>) {
        if (this.pendingResult != null) {
            this.pendingResult?.success(data)
            pendingReply = null
        } else {
            pendingReply = data
        }
    }

    private fun _handleResult(data: Intent?) {
        data?.let {
            val status = data.getIntExtra("status", -1)
            var isSuccess: Boolean = false
            if (status == MomoVnConfig.CODE_PAYMENT_SUCCESS) isSuccess = true
            val token = data.getStringExtra("data")
            val phonenumber = data.getStringExtra("phonenumber")
            val message = data.getStringExtra("message")
            var extra = data.getStringExtra("extra")
            if (extra == null) {
                extra = "";
            }
            val data: MutableMap<String, Any> = java.util.HashMap()
            data.put("isSuccess", isSuccess)
            data.put("status", status)
            data.put("phoneNumber", phonenumber.toString())
            data.put("token", token.toString())
            data.put("message", message.toString())
            data.put("extra", extra.toString())
            sendReply(data)
        } ?: run {
            val data: MutableMap<String, Any> = java.util.HashMap()
            data.put("isSuccess", false)
            data.put("status", 7);
            data.put("phoneNumber", "")
            data.put("token", "")
            data.put("message", "")
            data.put("extra", "")
            sendReply(data)
        }
    }
}