package com.configcat.configcatsample

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.configcat.*

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ConfigCatClient.newBuilder()
                .refreshPolicy { fetcher: ConfigFetcher, cache: ConfigCache -> AutoPollingPolicy.newBuilder()
                        .autoPollIntervalInSeconds(5)
                        .configurationChangeListener { parser, newConfiguration ->
                            run {
                                // create a user object to identify the caller
                                val user = User.newBuilder()
                                        .build("key")

                                var config = parser.parseValue(Boolean::class.java, newConfiguration, "bool30TrueAdvancedRules", user)
                                var textField = findViewById<TextView>(R.id.editText)
                                textField.text = "bool30TrueAdvancedRules: $config"
                            }
                        }
                        .build(fetcher, cache)}
                .build("PKDVCLf-Hq-h-kCzMp-L7Q/psuH7BGHoUmdONrzzUOY7A")
    }
}
