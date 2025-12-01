package com.rednote.ui.main.weather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.rednote.R
import com.rednote.data.model.weather.Cast
import com.rednote.data.model.weather.LiveWeather
import com.rednote.data.repository.LocationRepository
import com.rednote.databinding.FragmentWeatherBinding
import com.rednote.databinding.ItemForecastDayBinding
import com.rednote.ui.base.BaseFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class WeatherFragment : BaseFragment<FragmentWeatherBinding, WeatherViewModel>() {

    private val locationRepository = LocationRepository()
    private var loadingState: LoadingState = LoadingState.LOCATING
    private var isContentReady = false

    override val viewModel: WeatherViewModel by activityViewModels()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWeatherBinding {
        return FragmentWeatherBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        setupSwipeRefresh()
        // 初始状态：显示定位加载状态
        isContentReady = viewModel.liveWeatherData.value != null
        if (isContentReady) {
            loadingState = LoadingState.FETCHING
            hideLoadingUI()
            return
        }
        setLoadingState(LoadingState.LOCATING)
        showLoadingUI()
    }

    override fun initObservers() {
        // 观察天气数据
        viewModel.liveWeatherData.observe(viewLifecycleOwner) { live ->
            live?.let { updateWeatherUI(it) }
        }

        viewModel.forecastData.observe(viewLifecycleOwner) { casts ->
            if (!casts.isNullOrEmpty()) {
                updateForecastSummary(casts.first())
                updateForecastList(casts)
            } else {
                binding.forecastContainer.removeAllViews()
            }
        }

        viewModel.errorMsg.observe(viewLifecycleOwner) { msg ->
            hideLoadingUI()
            stopRefreshing()
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun initData() {
        // 检查是否需要更新数据
        if (viewModel.shouldUpdate()) {
            // 需要更新：初始化定位并获取新数据
            startLocation()
        } else {
            // 使用缓存数据：直接显示
            hideLoadingUI()
        }
    }

    override fun showLoadingUI() {
        binding.progressBar.visibility = View.VISIBLE
        binding.weatherContentGroup.visibility = View.GONE
        binding.tvLoadingHint.text = when (loadingState) {
            LoadingState.LOCATING -> getString(R.string.weather_loading_locating)
            LoadingState.FETCHING -> getString(R.string.weather_loading_fetching)
        }
    }

    override fun hideLoadingUI() {
        binding.progressBar.visibility = View.GONE
        binding.weatherContentGroup.visibility = if (isContentReady) View.VISIBLE else View.GONE
    }

    private fun startLocation(isPullToRefresh: Boolean = false) {
        if (isPullToRefresh) {
            binding.swipeRefreshLayout.isRefreshing = true
            showRefreshHint()
        } else {
            setLoadingState(LoadingState.LOCATING)
            showLoadingUI()
        }
        locationRepository.startSingleLocation(
            context = requireContext(),
            onSuccess = { adCode ->
                if (!isPullToRefresh) {
                    setLoadingState(LoadingState.FETCHING)
                    showLoadingUI()
                } else {
                    binding.tvRefreshTime.text = getRefreshHint()
                }
                // 如果是下拉刷新，则强制更新天气
                viewModel.fetchWeather(adCode, forceRefresh = isPullToRefresh)
            },
            onError = { message ->
                if (isPullToRefresh) {
                    stopRefreshing()
                } else {
                    hideLoadingUI()
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateWeatherUI(weather: LiveWeather) {
        isContentReady = true
        stopRefreshing()
        hideLoadingUI()

        binding.tvCityName.text = weather.city
        viewModel.cityName.value = weather.city
        binding.tvWeatherDesc.text = weather.weather
        binding.tvBigTemp.text = "${weather.temperature}°"
        binding.tvHumidity.text = "湿度: ${weather.humidity}%"
        binding.tvWind.text = "${weather.winddirection}风 ${weather.windpower}级"
    }

    private fun updateForecastSummary(cast: Cast) {
        val highTemp = cast.daytemp.toIntOrNull()
        val lowTemp = cast.nighttemp.toIntOrNull()
        val minTemp = if (highTemp != null && lowTemp != null) min(highTemp, lowTemp) else null
        val maxTemp = if (highTemp != null && lowTemp != null) max(highTemp, lowTemp) else null

        val rangeText = when {
            minTemp != null -> "${minTemp}°~${maxTemp}°"
            else -> "${cast.nighttemp}°~${cast.daytemp}°"
        }

        binding.tvTempRange.text = rangeText
    }

    private fun updateForecastList(casts: List<Cast>) {
        val inflater = LayoutInflater.from(requireContext())
        binding.forecastContainer.removeAllViews()

        casts.forEachIndexed { index, cast ->
            val itemBinding = ItemForecastDayBinding.inflate(inflater, binding.forecastContainer, false)
            itemBinding.tvForecastDate.text = formatForecastLabel(index, cast)
            itemBinding.tvForecastDesc.text = "${cast.dayweather} / ${cast.nightweather}"
            itemBinding.tvForecastTemp.text = "${cast.nighttemp}° ~ ${cast.daytemp}°"
            itemBinding.tvForecastWind.text = "${cast.daywind}风 ${cast.daypower}级"

            binding.forecastContainer.addView(itemBinding.root)
        }
    }

    private fun formatForecastLabel(index: Int, cast: Cast): String {
        return when (index) {
            0 -> "今天"
            1 -> "明天"
            else -> "${weekLabel(cast.week)} ${formatDate(cast.date)}"
        }
    }

    private fun weekLabel(week: String): String {
        return when (week) {
            "1" -> "星期一"
            "2" -> "星期二"
            "3" -> "星期三"
            "4" -> "星期四"
            "5" -> "星期五"
            "6" -> "星期六"
            "7" -> "星期日"
            else -> "星期?"
        }
    }

    private fun formatDate(date: String): String {
        return if (date.length >= 5) date.substring(5) else date
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationRepository.release()
    }

    private fun setLoadingState(state: LoadingState) {
        loadingState = state
    }

    private enum class LoadingState {
        LOCATING, FETCHING
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            startLocation(isPullToRefresh = true)
        }
    }

    private fun showRefreshHint() {
        binding.tvRefreshTime.visibility = View.VISIBLE
        binding.tvRefreshTime.text = getRefreshHint()
    }

    private fun stopRefreshing() {
        if (binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
        binding.tvRefreshTime.visibility = View.GONE
    }

    private fun getRefreshHint(): String {
        val lastTime = viewModel.lastUpdateTime
        return if (lastTime == 0L) {
            getString(R.string.weather_refresh_none)
        } else {
            val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            getString(R.string.weather_last_refresh_time, formatter.format(Date(lastTime)))
        }
    }
}
