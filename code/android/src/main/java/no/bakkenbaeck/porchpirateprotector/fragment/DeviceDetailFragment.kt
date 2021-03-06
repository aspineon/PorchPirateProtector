package no.bakkenbaeck.porchpirateprotector.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_device_detail.*
import kotlinx.coroutines.launch
import no.bakkenbaeck.porchpirateprotector.R
import no.bakkenbaeck.porchpirateprotector.extension.updateAnimating
import no.bakkenbaeck.porchpirateprotector.manager.KeyStoreManager
import no.bakkenbaeck.pppshared.model.PairedDevice
import no.bakkenbaeck.pppshared.presenter.DeviceDetailPresenter
import java.lang.RuntimeException

class DeviceDetailFragment: Fragment() {

    private val secureStorage: KeyStoreManager
        get() = KeyStoreManager(context!!)

    private val presenter by lazy { DeviceDetailPresenter(currentDevice) }

    private lateinit var currentDevice: PairedDevice

    companion object {
        const val ARG_DEVICE = "device_json"
    }

    // FRAGMENT LIFECYCLE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_device_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deviceJSON = arguments?.getString(ARG_DEVICE)

        deviceJSON?.let {
            val device = PairedDevice.fromJSONString(it)
            device?.let {
                currentDevice = it
            } ?: throw RuntimeException("JSON not parsable to a device!")
        } ?: throw RuntimeException("JSON not found!")

        textview_device_detail_name.text = presenter.title
        button_lock.setOnClickListener {
            presenter.launch {
                val viewState = presenter.lockAsync(
                    initialViewStateHandler = this@DeviceDetailFragment::configureForViewState,
                    secureStorage = secureStorage
                )

                configureForViewState(viewState)
            }
        }

        button_unlock.setOnClickListener {
            presenter.launch {
                val viewState = presenter.unlockAsync(
                    initialViewStateHandler = this@DeviceDetailFragment::configureForViewState,
                    secureStorage = secureStorage
                )

                configureForViewState(viewState)
            }
        }

        presenter.launch {
            val viewState = presenter.getStatusAsync(
                initialViewStateHandler = this@DeviceDetailFragment::configureForViewState,
                secureStorage = secureStorage
            )
            configureForViewState(viewState)
        }
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    // VIEW STATE CONFIGURATION

    private fun configureForViewState(viewState: DeviceDetailPresenter.DeviceDetailViewState) {
        button_lock.isEnabled = viewState.lockButtonEnabled
        button_unlock.isEnabled = viewState.unlockButtonEnabled
        textview_error_device_detail.text = viewState.errorMessage
        progress_bar_device_detail.updateAnimating(viewState.indicatorAnimating)
    }
}