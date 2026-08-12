package com.nia.pipeline.export

data class ExportSummary(

    val exportedFiles: Int,

    val exportedRecords: Int,

    val exportDirectory: String

)