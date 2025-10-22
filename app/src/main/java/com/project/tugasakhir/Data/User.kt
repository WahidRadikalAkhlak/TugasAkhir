package com.project.tugasakhir.Data

import java.io.Serializable


data class User(
    var id: String? = null,
    var email: String? = null,
    var nama: String? = null,
    var password: String? = null,
    var userAddress: String? = null
) : Serializable {

    constructor() : this(null, null, null, null)

}