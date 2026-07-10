package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiScannerService {
    private const val TAG = "GeminiScannerService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Decodes uri to bitmap safely and resizes to prevent massive payload sizes
    fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Calculate sample size to fit around 1024px maximum
            val reqWidth = 1024
            val reqHeight = 1024
            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val finalInputStream = context.contentResolver.openInputStream(uri) ?: return null
            val finalOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val bitmap = BitmapFactory.decodeStream(finalInputStream, null, finalOptions)
            finalInputStream.close()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding Uri to Bitmap: ${e.message}", e)
            null
        }
    }

    // Renders the first page of a PDF document into a Bitmap using Android SDK PdfRenderer
    fun renderPdfPageToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
            if (pdfRenderer.pageCount > 0) {
                val page = pdfRenderer.openPage(0)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pdfRenderer.close()
                parcelFileDescriptor.close()
                bitmap
            } else {
                pdfRenderer.close()
                parcelFileDescriptor.close()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering PDF to bitmap: ${e.message}")
            null
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun scanReceipt(bitmap: Bitmap): ParsedReceipt = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
            return@withContext ParsedReceipt(
                merchant = "Mock Cafe & Bistro",
                amount = 42.50,
                category = "Food & Grocery",
                date = "2026-07-09",
                isMock = true,
                errorMessage = "API key not configured in Secrets panel. Using simulated OCR scanner."
            )
        }

        val base64Image = bitmap.toBase64()
        val prompt = """
            You are an expert financial receipt scanner. Please analyze this receipt image.
            Extract the following details as a valid JSON object. Do not include any markdown backticks or formatting like ```json. Return ONLY the JSON object.
            Fields:
            - merchant (string): The merchant, cafe, store, or company name.
            - amount (number): The total amount charged (as a decimal double).
            - category (string): Must be one of: 'Food & Grocery', 'Investment', 'Shopping', 'Travelling', 'Bill & Subscription', 'Miscellaneous'.
            - date (string): Format YYYY-MM-DD. If not clear, default to today's date.
        """.trimIndent()

        // Build Gemini direct REST payload manually to keep imports slim and reliable
        val jsonPayload = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        // Part 1: Text prompt
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        // Part 2: Inline Image Data
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            // Force JSON response structure from the model
            val generationConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            }
            put("generationConfig", generationConfig)
        }

        val url = "$BASE_URL?key=$apiKey"
        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ParsedReceipt(
                    merchant = "Walmart",
                    amount = 89.99,
                    category = "Shopping",
                    date = "2026-07-09",
                    isMock = true,
                    errorMessage = "Server error ${response.code}: ${response.message}. Using offline preview."
                )
            }

            val bodyString = response.body?.string() ?: ""
            Log.d(TAG, "Response: $bodyString")

            val jsonResponse = JSONObject(bodyString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val textResponse = firstPart?.optString("text") ?: ""

            val cleanedText = textResponse.trim().removeSurrounding("```json", "```").trim()
            val parsedJson = JSONObject(cleanedText)

            ParsedReceipt(
                merchant = parsedJson.optString("merchant", "Unknown Merchant"),
                amount = parsedJson.optDouble("amount", 0.0),
                category = parsedJson.optString("category", "Miscellaneous"),
                date = parsedJson.optString("date", "2026-07-09"),
                isMock = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning receipt: ${e.message}", e)
            ParsedReceipt(
                merchant = "Mock Cafe & Bistro",
                amount = 42.50,
                category = "Food & Grocery",
                date = "2026-07-09",
                isMock = true,
                errorMessage = "Offline or scan error: ${e.localizedMessage}. Loaded in-memory preview."
            )
        }
    }

    suspend fun scanDocument(context: Context, uri: Uri, fileType: String): ParsedDocumentResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val isMockMode = apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()
        
        if (isMockMode) {
            val mockTxs = if (fileType.lowercase() == "csv") {
                listOf(
                    ParsedDocumentTransaction("Uber Trip CSV Sync", 24.50, "EXPENSE", "Travelling", "2026-07-08", "Weekly Commute"),
                    ParsedDocumentTransaction("Starbucks Coffee", 8.75, "EXPENSE", "Food & Grocery", "2026-07-09", "Weekly Commute"),
                    ParsedDocumentTransaction("Salary Wire Transfer", 3200.00, "INCOME", "Dine Out & Café", "2026-07-01", "Primary Income"),
                    ParsedDocumentTransaction("Robinhood Index Buy", 150.00, "INVESTMENT", "Investment", "2026-07-09", "Retirement Portfolio")
                )
            } else {
                listOf(
                    ParsedDocumentTransaction("Costco Wholesale Invoice", 189.43, "EXPENSE", "Food & Grocery", "2026-07-08", "Costco Bundle"),
                    ParsedDocumentTransaction("Target Home Goods", 45.20, "EXPENSE", "Shopping", "2026-07-09", "Costco Bundle"),
                    ParsedDocumentTransaction("Netflix Subscription", 15.49, "EXPENSE", "Bill & Subscription", "2026-07-05", "Monthly Entertainment")
                )
            }
            return@withContext ParsedDocumentResult(
                transactions = mockTxs,
                isMock = true,
                errorMessage = "API key not configured in Secrets. Simulated OCR document parsing."
            )
        }

        try {
            var base64Image: String? = null
            var textContent: String? = null
            
            if (fileType.lowercase() == "csv") {
                val inputStream = context.contentResolver.openInputStream(uri)
                textContent = inputStream?.bufferedReader()?.use { it.readText() }
            } else if (fileType.lowercase() == "pdf") {
                val bitmap = renderPdfPageToBitmap(context, uri)
                if (bitmap != null) {
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                } else {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    textContent = inputStream?.bufferedReader()?.use { it.readText() }
                }
            } else {
                val bitmap = decodeUriToBitmap(context, uri)
                if (bitmap != null) {
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                }
            }

            if (base64Image == null && textContent == null) {
                return@withContext ParsedDocumentResult(
                    transactions = emptyList(),
                    isMock = false,
                    errorMessage = "Could not parse or read document file format."
                )
            }

            val prompt = if (textContent != null) {
                """
                    You are an expert financial auditor and data extractor. Please analyze the following text or CSV data containing financial records.
                    Extract all individual transactions (expenses, income, investments) and return them as a valid JSON object. Do not include markdown formatting or backticks.
                    
                    Format of the JSON response:
                    {
                      "transactions": [
                        {
                          "title": "Merchant, Description or Title of transaction",
                          "amount": 42.50,
                          "type": "EXPENSE",
                          "category": "Food & Grocery",
                          "date": "YYYY-MM-DD",
                          "bundleName": "Optional Group Bundle Name"
                        }
                      ]
                    }
                    
                    Data content:
                    $textContent
                """.trimIndent()
            } else {
                """
                    You are an expert financial auditor. Please analyze this document image page.
                    Extract all individual financial transaction items (receipt rows, invoice items, billing list, or bank statement records) and return them as a valid JSON object. Do not include markdown formatting or backticks.
                    
                    Format of the JSON response:
                    {
                      "transactions": [
                        {
                          "title": "Merchant, Description or Title of transaction",
                          "amount": 12.99,
                          "type": "EXPENSE",
                          "category": "Food & Grocery",
                          "date": "YYYY-MM-DD",
                          "bundleName": "Optional Group Bundle Name"
                        }
                      ]
                    }
                """.trimIndent()
            }

            val jsonPayload = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            if (base64Image != null) {
                                put(JSONObject().apply {
                                    put("inlineData", JSONObject().apply {
                                        put("mimeType", "image/jpeg")
                                        put("data", base64Image)
                                    })
                                })
                            }
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                }
                put("generationConfig", generationConfig)
            }

            val url = "$BASE_URL?key=$apiKey"
            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ParsedDocumentResult(
                    transactions = emptyList(),
                    isMock = false,
                    errorMessage = "Server error ${response.code}: ${response.message}"
                )
            }

            val bodyString = response.body?.string() ?: ""
            val jsonResponse = JSONObject(bodyString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val textResponse = firstPart?.optString("text") ?: ""

            val cleanedText = textResponse.trim().removeSurrounding("```json", "```").trim()
            val parsedJson = JSONObject(cleanedText)
            val txsArray = parsedJson.optJSONArray("transactions") ?: JSONArray()
            
            val parsedTxs = ArrayList<ParsedDocumentTransaction>()
            for (i in 0 until txsArray.length()) {
                val item = txsArray.getJSONObject(i)
                parsedTxs.add(
                    ParsedDocumentTransaction(
                        title = item.optString("title", "Unknown"),
                        amount = item.optDouble("amount", 0.0),
                        type = item.optString("type", "EXPENSE"),
                        category = item.optString("category", "Miscellaneous"),
                        date = item.optString("date", "2026-07-09"),
                        bundleName = item.optString("bundleName", "").let { if (it.isBlank()) null else it }
                    )
                )
            }

            ParsedDocumentResult(transactions = parsedTxs, isMock = false)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing file document: ${e.message}", e)
            ParsedDocumentResult(
                transactions = emptyList(),
                isMock = false,
                errorMessage = "Scanning error: ${e.localizedMessage}"
            )
        }
    }
}

data class ParsedReceipt(
    val merchant: String,
    val amount: Double,
    val category: String,
    val date: String,
    val isMock: Boolean = false,
    val errorMessage: String? = null
)

data class ParsedDocumentTransaction(
    val title: String,
    val amount: Double,
    val type: String, // "EXPENSE", "INCOME", "INVESTMENT"
    val category: String,
    val date: String, // YYYY-MM-DD
    val bundleName: String? = null
)

data class ParsedDocumentResult(
    val transactions: List<ParsedDocumentTransaction>,
    val isMock: Boolean = false,
    val errorMessage: String? = null
)
