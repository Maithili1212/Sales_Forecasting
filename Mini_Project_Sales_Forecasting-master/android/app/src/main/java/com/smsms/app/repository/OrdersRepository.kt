package com.smsms.app.repository

import com.smsms.app.model.Batch
import com.smsms.app.model.CreateBatchRequest
import com.smsms.app.model.CreateOrderRequest
import com.smsms.app.model.CreatePaymentRequest
import com.smsms.app.model.CreateQualityLogRequest
import com.smsms.app.model.Order
import com.smsms.app.model.Payment
import com.smsms.app.model.QualityLog
import com.smsms.app.model.SalesForecast
import com.smsms.app.model.SalesForecastRunResponse
import com.smsms.app.model.UpdateBatchStatusRequest
import com.smsms.app.model.UpdatePaymentStatusRequest
import com.smsms.app.network.ApiClient
import com.smsms.app.network.safeApiCall
import com.smsms.app.util.ApiResult

object OrdersRepository {

    private val api = ApiClient.api

    suspend fun createOrder(request: CreateOrderRequest): ApiResult<Order> = safeApiCall { api.createOrder(request) }

    suspend fun listOrders(): ApiResult<List<Order>> = safeApiCall { api.listOrders() }

    suspend fun getOrder(orderId: Int): ApiResult<Order> = safeApiCall { api.getOrder(orderId) }

    suspend fun closeOrder(orderId: Int): ApiResult<Order> = safeApiCall { api.closeOrder(orderId) }

    suspend fun createBatch(orderId: Int, quantity: Int): ApiResult<Batch> =
        safeApiCall { api.createBatch(orderId, CreateBatchRequest(quantity)) }

    suspend fun updateBatchStatus(batchId: Int, status: String): ApiResult<Batch> =
        safeApiCall { api.updateBatchStatus(batchId, UpdateBatchStatusRequest(status)) }

    suspend fun createQualityLog(batchId: Int, qtyChecked: Int, defectCount: Int): ApiResult<QualityLog> =
        safeApiCall { api.createQualityLog(batchId, CreateQualityLogRequest(qtyChecked, defectCount)) }

    suspend fun listQualityLogs(batchId: Int): ApiResult<List<QualityLog>> =
        safeApiCall { api.listQualityLogs(batchId) }

    suspend fun runSalesForecast(): ApiResult<SalesForecastRunResponse> =
        safeApiCall { api.runSalesForecast() }

    suspend fun listSalesForecast(): ApiResult<List<SalesForecast>> =
        safeApiCall { api.listSalesForecast() }

    suspend fun createPayment(orderId: Int, amount: Double, status: String?): ApiResult<Payment> =
        safeApiCall { api.createPayment(orderId, CreatePaymentRequest(amount, status)) }

    suspend fun updatePaymentStatus(paymentId: Int, status: String): ApiResult<Payment> =
        safeApiCall { api.updatePaymentStatus(paymentId, UpdatePaymentStatusRequest(status)) }
}
