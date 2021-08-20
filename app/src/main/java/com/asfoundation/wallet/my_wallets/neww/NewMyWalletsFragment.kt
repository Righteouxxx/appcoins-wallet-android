package com.asfoundation.wallet.my_wallets.neww

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.asf.wallet.R
import com.asf.wallet.databinding.FragmentMyWalletsBinding
import com.asfoundation.wallet.base.Async
import com.asfoundation.wallet.base.SingleStateFragment
import com.asfoundation.wallet.my_wallets.neww.list.OtherWalletsClick
import com.asfoundation.wallet.my_wallets.neww.list.OtherWalletsController
import com.asfoundation.wallet.ui.MyAddressActivity
import com.asfoundation.wallet.ui.balance.BalanceScreenModel
import com.asfoundation.wallet.ui.balance.BalanceVerificationModel
import com.asfoundation.wallet.ui.balance.BalanceVerificationStatus
import com.asfoundation.wallet.ui.balance.TokenBalance
import com.asfoundation.wallet.ui.iab.FiatValue
import com.asfoundation.wallet.ui.wallets.WalletBalance
import com.asfoundation.wallet.ui.wallets.WalletsModel
import com.asfoundation.wallet.util.*
import com.asfoundation.wallet.viewmodel.BasePageViewFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.qr_code_layout.*
import java.math.BigDecimal
import javax.inject.Inject

class NewMyWalletsFragment : BasePageViewFragment(),
    SingleStateFragment<MyWalletsState, MyWalletsSideEffect> {

  @Inject
  lateinit var viewModelFactory: MyWalletsViewModelFactory

  @Inject
  lateinit var formatter: CurrencyFormatUtils

  @Inject
  lateinit var navigator: NewMyWalletsNavigator

  private val viewModel: MyWalletsViewModel by viewModels { viewModelFactory }
  private val views by viewBinding(FragmentMyWalletsBinding::bind)

  private lateinit var otherWalletsController: OtherWalletsController

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_my_wallets, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initializeView()
    setListeners()
    viewModel.collectStateAndEvents(lifecycle, viewLifecycleOwner.lifecycleScope)
  }

  private fun initializeView() {
    otherWalletsController = OtherWalletsController()
    otherWalletsController.walletClickListener = { click ->
      when (click) {
        OtherWalletsClick.CreateNewWallet -> navigator.navigateToCreateNewWallet()
        is OtherWalletsClick.OtherWalletClick -> navigator.navigateToChangeActiveWallet(
            click.walletBalance)
      }
    }
    views.otherWalletsRecyclerView.setController(otherWalletsController)
  }

  private fun setListeners() {
    views.appcoinsCardView.setOnClickListener {
      val title = "${getString(R.string.appc_token_name)} (${
        getString(R.string.p2p_send_currency_appc)
      })"
      val image = requireContext().getDrawableURI(R.drawable.ic_appc)
      val description = getString(R.string.balance_appcoins_body)
      navigator.navigateToTokenInfo(title, image, description, false)
    }

    views.appcoinsCreditsCardView.setOnClickListener {
      val title = "${getString(R.string.appc_credits_token_name)} (${
        getString(R.string.p2p_send_currency_appc_c)
      })"
      val image = requireContext().getDrawableURI(R.drawable.ic_appc_c_token)
      val description = getString(R.string.balance_appccreditos_body)
      navigator.navigateToTokenInfo(title, image, description, true)
    }

    views.ethCardView.setOnClickListener {
      val title = "${getString(R.string.ethereum_token_name)} (${
        getString(R.string.p2p_send_currency_eth)
      })"
      val image = requireContext().getDrawableURI(R.drawable.ic_eth_token)
      val description = getString(R.string.balance_ethereum_body)
      navigator.navigateToTokenInfo(title, image, description, false)
    }

    views.actionButtonMore.setOnClickListener { navigateToMore() }

    views.backupButton.setOnClickListener {
      viewModel.state.walletsAsync()
          ?.let { walletsModel ->
            navigator.navigateToBackupWallet(walletsModel.currentWallet.walletAddress)
          }
    }
    views.qrImage.setOnClickListener {
      viewModel.state.walletsAsync()
          ?.let { walletsModel ->
            navigator.navigateToQrCode(views.qrImage)
          }
    }
    views.verifyButton.setOnClickListener { navigator.navigateToVerify() }
    views.insertCodeButton.setOnClickListener { navigator.navigateToVerify() }
  }

  override fun onStateChanged(state: MyWalletsState) {
    setWallets(state.walletsAsync)
    setVerified(state.walletVerifiedAsync)
    setBalance(state.balanceAsync)
    setWalletCreation(state.walletCreationAsync)
    setBackupTooltip(state.backedUpOnceAsync)
  }

  override fun onSideEffect(sideEffect: MyWalletsSideEffect) = Unit


  private fun setBackupTooltip(backedUpOnceAsync: Async<Boolean>) {
    backedUpOnceAsync()?.let { backedUpOnce ->
      views.backupWalletCardView.visibility = if (backedUpOnce) View.GONE else View.VISIBLE
    }
  }

  private fun setWallets(walletAsync: Async<WalletsModel>) {
    when (walletAsync) {
      is Async.Success -> {
        val walletsModel = walletAsync()
        setCurrentWallet(walletsModel.currentWallet)
        setOtherWallets(walletsModel.otherWallets)
      }
      else -> Unit
    }
  }

  private fun setOtherWallets(otherWallets: List<WalletBalance>) {
    otherWalletsController.setData(otherWallets)
  }

  private fun setCurrentWallet(currentWallet: WalletBalance) {
    views.walletAddressTextView.text = currentWallet.walletAddress
    views.actionButtonShareAddress.setOnClickListener {
      showShare(currentWallet.walletAddress)
    }
    views.actionButtonCopyAddress.setOnClickListener {
      setAddressToClipBoard(currentWallet.walletAddress)
    }

    setQrCode(currentWallet.walletAddress)
  }

  private fun setBalance(balanceAsync: Async<BalanceScreenModel>) {
    when (balanceAsync) {
      Async.Uninitialized,
      is Async.Loading -> {
        views.appccValueSkeleton.playAnimation()
        views.appccValueSkeleton.visibility = View.VISIBLE
        views.appcValueSkeleton.playAnimation()
        views.appcValueSkeleton.visibility = View.VISIBLE
        views.ethValueSkeleton.playAnimation()
        views.ethValueSkeleton.visibility = View.VISIBLE
        views.totalBalanceSkeleton.playAnimation()
        views.totalBalanceSkeleton.visibility = View.VISIBLE
      }
      is Async.Fail -> {
      }
      is Async.Success -> {
        val balanceScreenModel = balanceAsync()
        updateTokenValue(balanceScreenModel.appcBalance, WalletCurrency.APPCOINS)
        updateTokenValue(balanceScreenModel.creditsBalance, WalletCurrency.CREDITS)
        updateTokenValue(balanceScreenModel.ethBalance, WalletCurrency.ETHEREUM)
        updateOverallBalance(balanceScreenModel.overallFiat)
      }
    }
  }

  private fun setWalletCreation(walletCreationAsync: Async<Unit>) {
    when (walletCreationAsync) {
      is Async.Loading -> TODO()
      is Async.Success -> TODO()
      else -> Unit
    }
  }

  private fun updateTokenValue(balance: TokenBalance, tokenCurrency: WalletCurrency) {
    val tokenBalance = getTokenValueText(balance, tokenCurrency)
    if (tokenBalance != "-1") {
      when (tokenCurrency) {
        WalletCurrency.CREDITS -> {
          views.appccValueSkeleton.visibility = View.GONE
          views.appccValueSkeleton.cancelAnimation()
          views.appccValue.text = tokenBalance
          views.appccValue.visibility = View.VISIBLE
        }
        WalletCurrency.APPCOINS -> {
          views.appcValueSkeleton.visibility = View.GONE
          views.appcValueSkeleton.cancelAnimation()
          views.appcValue.text = tokenBalance
          views.appcValue.visibility = View.VISIBLE
        }
        WalletCurrency.ETHEREUM -> {
          views.ethValueSkeleton.visibility = View.GONE
          views.ethValueSkeleton.cancelAnimation()
          views.ethValue.text = tokenBalance
          views.ethValue.visibility = View.VISIBLE
        }
        else -> return
      }
    }
  }

  private fun getFiatBalanceText(balance: FiatValue): String {
    var overallBalance = "-1"
    if (balance.amount.compareTo(BigDecimal("-1")) == 1) {
      overallBalance = formatter.formatCurrency(balance.amount)
    }
    if (overallBalance != "-1") {
      return balance.symbol + overallBalance
    }
    return overallBalance
  }

  private fun getTokenValueText(balance: TokenBalance, tokenCurrency: WalletCurrency): String {
    var tokenBalance = "-1"
    val fiatCurrency = balance.fiat.symbol
    if (balance.token.amount.compareTo(BigDecimal("-1")) == 1) {
      tokenBalance = formatter.formatCurrency(balance.token.amount, tokenCurrency)
    }
    return tokenBalance
  }

  private fun updateOverallBalance(balance: FiatValue) {
    val overallBalance = getFiatBalanceText(balance)
    if (overallBalance != "-1") {
      views.totalBalanceSkeleton.visibility = View.GONE
      views.totalBalanceSkeleton.cancelAnimation()
      views.totalBalanceTextView.text = overallBalance
      views.totalBalanceTextView.visibility = View.VISIBLE
    }
  }

  private fun setVerified(walletVerifiedAsync: Async<BalanceVerificationModel>) {
    when (walletVerifiedAsync) {
      is Async.Success -> {
        val verifiedModel = walletVerifiedAsync()
        when (verifiedModel.status) {
          BalanceVerificationStatus.VERIFIED -> setVerifiedWallet(true)
          BalanceVerificationStatus.UNVERIFIED -> setVerifiedWallet(false)
          BalanceVerificationStatus.CODE_REQUESTED -> setShowVerifyInsertCode()
          BalanceVerificationStatus.NO_NETWORK,
          BalanceVerificationStatus.ERROR -> {
            // Set cached value
            when (verifiedModel.cachedStatus) {
              BalanceVerificationStatus.VERIFIED -> setVerifiedWallet(true)
              BalanceVerificationStatus.UNVERIFIED -> setVerifiedWallet(verified = false,
                  disableButton = true)
              BalanceVerificationStatus.CODE_REQUESTED -> setShowVerifyInsertCode(true)
              else -> setVerifiedWallet(verified = false, disableButton = true)
            }
          }
          null -> {
            // Set cached value
            when (verifiedModel.cachedStatus) {
              BalanceVerificationStatus.VERIFIED -> setVerifiedWallet(true)
              BalanceVerificationStatus.UNVERIFIED -> setVerifiedWallet(false)
              BalanceVerificationStatus.CODE_REQUESTED -> setShowVerifyInsertCode()
              else -> setVerifiedWallet(false)
            }
          }
        }
      }
      else -> Unit
    }
  }

  private fun setVerifiedWallet(verified: Boolean, disableButton: Boolean = false) {
    views.insertCodeWalletCardView.visibility = View.GONE
    if (verified) {
      views.verifyWalletCardView.visibility = View.GONE
      views.verifiedWalletText.visibility = View.VISIBLE
      views.verifiedWalletIcon.visibility = View.VISIBLE
    } else {
      views.verifyButton.isEnabled = !disableButton
      views.verifyWalletCardView.visibility = View.VISIBLE
      views.verifiedWalletText.visibility = View.GONE
      views.verifiedWalletIcon.visibility = View.GONE
    }
  }

  private fun navigateToMore() {
    safeLet(viewModel.state.balanceAsync(),
        viewModel.state.walletsAsync()) { balanceScreenModel, walletsModel ->
      val overallFiatValue = getFiatBalanceText(balanceScreenModel.overallFiat)
      val appcoinsValue = "${
        getTokenValueText(balanceScreenModel.appcBalance, WalletCurrency.APPCOINS)
      } ${balanceScreenModel.appcBalance.token.symbol}"
      val creditsValue = "${
        getTokenValueText(balanceScreenModel.creditsBalance, WalletCurrency.CREDITS)
      } ${balanceScreenModel.creditsBalance.token.symbol}"
      val ethValue = "${
        getTokenValueText(balanceScreenModel.ethBalance, WalletCurrency.ETHEREUM)
      } ${balanceScreenModel.ethBalance.token.symbol}"
      navigator.navigateToMore(walletsModel.currentWallet.walletAddress, overallFiatValue,
          appcoinsValue, creditsValue, ethValue, walletsModel.otherWallets.isNotEmpty())
    }
  }

  private fun setShowVerifyInsertCode(disableButton: Boolean = false) {
    views.verifyWalletCardView.visibility = View.GONE
    views.verifiedWalletText.visibility = View.GONE
    views.verifiedWalletIcon.visibility = View.GONE
    views.insertCodeWalletCardView.visibility = View.VISIBLE
    views.insertCodeButton.isEnabled = !disableButton
  }

  private fun setQrCode(walletAddress: String) {
    // Up screen brightness
//    val params = requireActivity().window.attributes.apply { screenBrightness = 1f }
//    requireActivity().window.attributes = params
//    requireActivity().window.addFlags(WindowManager.LayoutParams.FLAGS_CHANGED)
    try {
      val logo = ResourcesCompat.getDrawable(resources, R.drawable.ic_appc_token, null)
      val mergedQrCode = walletAddress.generateQrCode(requireActivity().windowManager, logo!!)
      views.qrImage.setImageBitmap(mergedQrCode)
    } catch (e: Exception) {
      Snackbar.make(main_layout, getString(R.string.error_fail_generate_qr), Snackbar.LENGTH_SHORT)
          .show()
    }
  }

  fun setAddressToClipBoard(walletAddress: String) {
    val clipboard =
        requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    val clip = ClipData.newPlainText(
        MyAddressActivity.KEY_ADDRESS, walletAddress)
    clipboard?.setPrimaryClip(clip)
    val bottomNavView: BottomNavigationView = requireActivity().findViewById(R.id.bottom_nav)!!

    Snackbar.make(bottomNavView, R.string.wallets_address_copied_body, Snackbar.LENGTH_SHORT)
        .apply {
          anchorView = bottomNavView
        }
        .show()
  }

  fun showShare(walletAddress: String) {
    ShareCompat.IntentBuilder.from(requireActivity())
        .setText(walletAddress)
        .setType("text/plain")
        .setChooserTitle(resources.getString(R.string.share_via))
        .startChooser()

  }
}