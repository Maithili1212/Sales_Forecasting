package com.smsms.app.network

import com.smsms.app.model.Batch
import com.smsms.app.model.CreateBatchRequest
import com.smsms.app.model.CreateOrderRequest
import com.smsms.app.model.CreateQualityLogRequest
import com.smsms.app.model.LoginRequest
import com.smsms.app.model.LoginResponse
import com.smsms.app.model.CreatePaymentRequest
import com.smsms.app.model.Order
import com.smsms.app.model.Payment
import com.smsms.app.model.QualityLog
import com.smsms.app.model.RegisterRequest
import com.smsms.app.model.SalesForecast
import com.smsms.app.model.SalesForecastRunResponse
import com.smsms.app.model.UpdateBatchStatusRequest
import com.smsms.app.model.UpdateOrderRequest
import com.smsms.app.model.UpdatePaymentStatusRequest
import com.smsms.app.model.UserSummary
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// One function per backend endpoint. Retrofit generates the actual HTTP
// call implementation from these annotations at runtime — this interface
// is never implemented by hand.
interface SmsmsApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<UserSummary>

    @POST("api/orders")
    suspend fun createOrder(@Body body: CreateOrderRequest): Response<Order>

    @GET("api/orders")
    suspend fun listOrders(
        @Query("status") status: String? = null,
        @Query("customer_name") customerName: String? = null
    ): Response<List<Order>>

    @GET("api/orders/{orderId}")
    suspend fun getOrder(@Path("orderId") orderId: Int): Response<Order>

    @PATCH("api/orders/{orderId}")
    suspend fun updateOrder(@Path("orderId") orderId: Int, @Body body: UpdateOrderRequest): Response<Order>

    @PATCH("api/orders/{orderId}/close")
    suspend fun closeOrder(@Path("orderId") orderId: Int): Response<Order>

    @POST("api/orders/{orderId}/batches")
    suspend fun createBatch(@Path("orderId") orderId: Int, @Body body: CreateBatchRequest): Response<Batch>

    @GET("api/orders/{orderId}/batches")
    suspend fun listBatches(@Path("orderId") orderId: Int): Response<List<Batch>>

    @PATCH("api/batches/{batchId}/status")
    suspend fun updateBatchStatus(@Path("batchId") batchId: Int, @Body body: UpdateBatchStatusRequest): Response<Batch>

    @POST("api/batches/{batchId}/quality-logs")
    suspend fun createQualityLog(@Path("batchId") batchId: Int, @Body body: CreateQualityLogRequest): Response<QualityLog>

    @GET("api/batches/{batchId}/quality-logs")
    suspend fun listQualityLogs(@Path("batchId") batchId: Int): Response<List<QualityLog>>

    @POST("api/sales-forecast/run")
    suspend fun runSalesForecast(): Response<SalesForecastRunResponse>

    @GET("api/sales-forecast")
    suspend fun listSalesForecast(): Response<List<SalesForecast>>

    @POST("api/orders/{orderId}/payments")
    suspend fun createPayment(@Path("orderId") orderId: Int, @Body body: CreatePaymentRequest): Response<Payment>

    @PATCH("api/payments/{paymentId}/status")
    suspend fun updatePaymentStatus(@Path("paymentId") paymentId: Int, @Body body: UpdatePaymentStatusRequest): Response<Payment>
}
