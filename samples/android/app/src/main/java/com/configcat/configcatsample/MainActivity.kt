package com.configcat.configcatsample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.configcat.AutoPollingPolicy
import com.configcat.ConfigCatClient
import com.configcat.ConfigCache
import com.configcat.ConfigFetcher

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ConfigCatClient.newBuilder()
                .refreshPolicy({fetcher: ConfigFetcher, cache: ConfigCache -> AutoPollingPolicy.newBuilder()
                        .autoPollRateInSeconds(5)
                        .configurationChangeListener({parser, newConfiguration ->
                            run {
                                // create a user object to identify the caller
                                val user = User.newBuilder()
                                        .build("key")

                                var config = parser.parse(Sample::class.java, newConfiguration, user)
                                var textField = findViewById<TextView>(R.id.editText)
                                textField.text = "bool30TrueAdvancedRules: " + config.bool30TrueAdvancedRules + "\n" +
                                        "integer25One25Two25Three25FourAdvancedRules: " + config.keyInteger + "\n" +
                                        "double25Pi25E25Gr25Zero: " + config.double25Pi25E25Gr25Zero + "\n" +
                                        "string25Cat25Dog25Falcon25Horse: " + config.keyString
                            }
                        })
                        .build(fetcher, cache)})
                .build("PKDVCLf-Hq-h-kCzMp-L7Q/psuH7BGHoUmdONrzzUOY7A")
    }

    data class Sample(val bool30TrueAdvancedRules: Boolean = false,
                      val integer25One25Two25Three25FourAdvancedRules: Int = 0,
                      val double25Pi25E25Gr25Zero: Double = 0.0,
                      val string25Cat25Dog25Falcon25Horse: String = "")
}
