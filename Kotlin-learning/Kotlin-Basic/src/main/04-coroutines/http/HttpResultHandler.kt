package http

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import io.reactivex.functions.Function
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.Optional


suspend fun main() {
    //testAPICall()
    //testNullableAPICall()
    //testRxJavaCall()
    testReflecting()
}

///////////////////////////////////////////////////////////////////////////
// API Call
///////////////////////////////////////////////////////////////////////////
private val gson = Gson()

private interface AccountApi {

    @POST("user/v1/client/login")
    suspend fun login(@Body loginRequest: LoginRequest): HttpResult<LoginResponse>

    @POST("user/v1/client/login")
    fun loginRx(@Body loginRequest: LoginRequest): Observable<HttpResult<LoginResponse?>>

    @POST("user/v1/client/login")
    suspend fun loginNullable(@Body loginRequest: LoginRequest): HttpResult<LoginResponse?>

}

private suspend fun testAPICall() {
    try {
        val loginResponse = callAPI(buildRealCall(), true)
        println("testAPICall-success: $loginResponse")
    } catch (e: Exception) {
        println("testAPICall-error: $e")
    }
}

private suspend fun testNullableAPICall() {
    try {
        val loginResponse = callAPI(buildRealCallNullable(), false)
        println("testNullableAPICall-success: $loginResponse")
    } catch (e: Exception) {
        println("testNullableAPICall-error: $e")
    }
}

fun testRxJavaCall() {
    serverAPI.loginRx(request).onErrorResumeNext(Function<Throwable, Observable<HttpResult<LoginResponse?>>> {
        Observable.error(
            transformHttpException(it)
        )
    }).map {
        if (!it.isSuccess) {
            onApiError(it)
            throw createApiException(it)
        }
        if (it.data == null) {
            Optional.empty<LoginResponse>()
        } else {
            Optional.of<LoginResponse>(it.data)
        }
    }.subscribe({
        println("testRxJavaCall-success: $it")
    }, {
        println("testRxJavaCall-error: $it")
    })
}

///////////////////////////////////////////////////////////////////////////
// ??????????????????
///////////////////////////////////////////////////////////////////////////
private suspend fun <T> callAPI(
    call: suspend () -> Result<T>, requireNullData: Boolean
): T {

    val result: Result<T>

    try {
        result = call.invoke()
    } catch (throwable: Throwable) {
        throw transformHttpException(throwable)
    }

    println("callAPI result = $result")
    return if (!result.isSuccess) { //???????????????????????????
        onApiError(result)
        throw createApiException(result)
    } else {
        if (requireNullData && result.data == null) {
            throw ServerErrorException(ServerErrorException.NO_DATA_ERROR)
        }
        result.data
    }
}

private fun <T> createApiException(
    result: Result<T>
): Throwable {
    return APIErrorException(result.code, result.message)
}

private fun <T> onApiError(result: Result<T>) {
    println("onApiError")
}

private fun transformHttpException(throwable: Throwable): Throwable {
    println("transformHttpException $throwable")
    return if (throwable is HttpException && !(throwable.code() >= 500)) {
        val errorBody = throwable.response()?.errorBody()
        if (errorBody == null) {
            throwable
        } else {
            parseErrorBody(errorBody.string()) ?: throwable
        }
    } else throwable
}

private fun parseErrorBody(string: String): APIErrorException? {
    println("parseErrorBody: $string")
    return try {
        val errorResult = gson.fromJson(string, ErrorResult::class.java)
        APIErrorException(errorResult.status, errorResult.msg)
    } catch (e: Exception) {
        null
    }
}

private class APIErrorException(
    private val code: Int, message: String
) : Exception(message) {
    override fun toString(): String {
        return "APIErrorException(code=$code, message=${message})"
    }
}

class ServerErrorException(private val serverDataError: Int) : RuntimeException("???????????????") {
    companion object {
        const val SERVER_DATA_ERROR = 1
        const val NO_DATA_ERROR = 3
    }

    override fun toString(): String {
        return "ServerErrorException(code=$serverDataError, message=${message})"
    }
}

///////////////////////////////////////////////////////////////////////////
// ??????
///////////////////////////////////////////////////////////////////////////
private val request = gson.fromJson(
    "{\"client\":\"1\",\"diskName\":\"Mi 10\",\"imei\":\"2371FC1256A3C83EC681D85AA831B75FA053C49E\",\"ipAddr\":\"10.5.12.162\",\"mac\":\"\",\"password\":\"Kl2NKoLUjKLn04irlJfdQQ==\",\"phone\":\"19999999999\",\"uuid\":\"2371FC1256A3C83EC681D85AA831B75FA053C49E\"}",
    LoginRequest::class.java
)

private fun buildRealCall(): suspend () -> Result<LoginResponse> {
    val networkCall: suspend () -> Result<LoginResponse> = {
        serverAPI.login(request)
    }
    return networkCall
}

private fun buildRealCallNullable(): suspend () -> Result<LoginResponse?> {
    val networkCall: suspend () -> Result<LoginResponse?> = {
        serverAPI.loginNullable(request)
    }
    return networkCall
}

//?????????
//private const val FAKE_BODY = "{\"status\":0,\"msg\":\"??????\"}"
//private const val FAKE_BODY = "{\"status\":0,\"msg\":\"??????\",\"data\":null}"
//private const val FAKE_BODY = "{\"status\":0,\"msg\":\"??????\",\"data\":[]}"
private const val FAKE_BODY = "{\"status\":0,\"msg\":\"??????\",\"data\":{}}"
//private const val FAKE_BODY = "{\"status\":20,\"msg\":\"API ??????\"}"

private val serverAPI by lazy {

    val httpLoggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
        override fun log(message: String) {
            println(message)
        }
    })

    httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

    val retrofit = retrofit2.Retrofit.Builder().client(
        OkHttpClient.Builder()
            //??????
            .addInterceptor(httpLoggingInterceptor)
            //??????
            .addInterceptor(Interceptor {
                val request = it.request()
                val newBuilder = request.newBuilder()
                with(newBuilder) {
                    header("os", "android")
                    header("versionName", "2.0.1")
                    header("versionCode", "201")
                    header("devicesId", "2371FC1256A3C83EC681D85AA831B75FA053C49E")
                    header("brand", "XiaoMi")
                    //header("Authorization", "")
                }
                it.proceed(newBuilder.build())
            })
            //??????
            .addInterceptor {
                val response = it.proceed(it.request())
                response.newBuilder().code(200).message("OK").body(FAKE_BODY.toResponseBody())
                    //response.newBuilder().code(455).message("Internal Error").body(FAKE_BODY.toResponseBody())
                    .build()
            }.build()
    ).baseUrl("http://demo.ysj.vclusters.com/api/")
        //json
        .addConverterFactory(ErrorJsonLenientConverterFactory(GsonConverterFactory.create(gson)))
        //rx call
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create()).build()

    retrofit.create(AccountApi::class.java)
}

private data class LoginRequest(
    val phone: String,
    val password: String,
    val diskName: String,
    val imei: String,
    val uuid: String,
    val mac: String,
    val ipAddr: String,
    val client: String = "1",
)

data class LoginResponse(
    val id: Long = -1,
    val username: String = "",
    val token: String = "",
    val nextCloudIP: String = "",
    val nextLocalNetworkIP: String = "",
)

///////////////////////////////////////////////////////////////////////////
// ????????????
///////////////////////////////////////////////////////////////////////////

private interface Result<T> {
    val data: T
    val code: Int
    val message: String
    val isSuccess: Boolean
}

/**
 * 1. ????????????????????????????????? GSON ?????? Unsafe ???????????????
 * 2. T ??????????????? Any??????????????????? Java ????????? getData ???????????????  null ????????????????????????????????? HttpResult
 *      ???????????????????????????????????????????????????????????? null?????????????????????????????????????????? HttpResult ?????? null ???????????????
 */
data class HttpResult<T>(
    @SerializedName("data") override val data: T,
    @SerializedName("status") override val code: Int,
    @SerializedName("msg") override val message: String
) : Result<T> {

    init {
        println("HttpResult Constructor Called.")
    }

    override val isSuccess: Boolean
        get() = code == 0
}

data class ErrorResult(
    val status: Int = 0,
    val msg: String = "",
)

///////////////////////////////////////////////////////////////////////////
// ????????????????????????
///////////////////////////////////////////////////////////////////////////

private fun testReflecting() {
    val type = object : TypeToken<HttpResult<Void>>() {

    }.type

    println(type)

    val data: HttpResult<String> = gson.fromJson(FAKE_BODY, type)

    println(data)
}

