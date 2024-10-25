package io.nekohasekai.sagernet.vpn.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.LottiProgressBarAnimationBinding

class LottiProgressBarAnimation @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: LottiProgressBarAnimationBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = LottiProgressBarAnimationBinding.inflate(inflater, this, true)
        binding.laProgressBarAnimation.visibility = View.GONE
    }

    private fun changeProgressBarColor(color: Int) {
        val keyPath = KeyPath("**")
        binding.laProgressBarAnimation.addValueCallback(
            keyPath,
            LottieProperty.COLOR_FILTER,
            LottieValueCallback(PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN))
        )
    }

    fun playLoadingAnimation() {
        val color = ContextCompat.getColor(context, R.color.lightBlue)
        changeProgressBarColor(color)
        binding.laProgressBarAnimation.visibility = View.VISIBLE
        binding.laProgressBarAnimation.setMinAndMaxFrame(0, 239)
        binding.laProgressBarAnimation.repeatCount = 2
        binding.laProgressBarAnimation.playAnimation()
    }

    fun playLoginErrorAnimation(onAnimationEnd: () -> Unit) {
        val color = ContextCompat.getColor(context, R.color.material_red_600)
        changeProgressBarColor(color)
        binding.laProgressBarAnimation.visibility = View.VISIBLE
        binding.laProgressBarAnimation.setMinAndMaxFrame(670, 840)
        binding.laProgressBarAnimation.repeatCount = 1
        binding.laProgressBarAnimation.playAnimation()

        // Set a listener to know when the animation ends
        binding.laProgressBarAnimation.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                binding.laProgressBarAnimation.visibility = View.GONE
                onAnimationEnd()
            }
        })
    }
}
