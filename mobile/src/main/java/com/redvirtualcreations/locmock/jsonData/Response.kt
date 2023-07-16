package com.redvirtualcreations.locmock.jsonData

import com.google.gson.annotations.SerializedName

data class Response(

	@field:SerializedName("results")
	val results: List<ResultsItem?>? = null
)

data class ResultsItem(

	@field:SerializedName("elevation")
	val elevation: Int? = null,

	@field:SerializedName("latitude")
	val latitude: Any? = null,

	@field:SerializedName("longitude")
	val longitude: Any? = null
)
