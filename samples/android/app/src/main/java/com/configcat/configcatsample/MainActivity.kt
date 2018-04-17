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
                                var config = parser.parse(Sample::class.java, newConfiguration)
                                var textField = findViewById<TextView>(R.id.editText)
                                textField.text = "keyBool: " + config.keyBool + "\n" +
                                        "keyInteger: " + config.keyInteger + "\n" +
                                        "keyDouble: " + config.keyDouble + "\n" +
                                        "keyString: " + config.keyString
                            }
                        })
                        .build(fetcher, cache)})
                .build("PKDVCLf-Hq-h-kCzMp-L7Q/PaDVCFk9EpmD6sLpGLltTA")
    }

    data class Sample(val keyBool: Boolean = false, val keyInteger: Int = 0, val keyDouble: Double = 0.0, val keyString: String = "")
}
