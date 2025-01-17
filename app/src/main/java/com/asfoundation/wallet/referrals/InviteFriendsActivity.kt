package com.asfoundation.wallet.referrals

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.*
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import com.asf.wallet.R
import com.asfoundation.wallet.router.ExternalBrowserRouter
import com.asfoundation.wallet.ui.BaseActivity
import com.asfoundation.wallet.verification.ui.credit_card.VerificationCreditCardActivity
import com.asfoundation.wallet.wallets.FindDefaultWalletInteract
import com.jakewharton.rxbinding2.view.RxView
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import kotlinx.android.synthetic.main.invite_friends_activity_layout.*
import kotlinx.android.synthetic.main.no_network_retry_only_layout.*
import java.math.BigDecimal
import javax.inject.Inject

class InviteFriendsActivity : BaseActivity(), InviteFriendsActivityView {

  private lateinit var menu: Menu
  private lateinit var presenter: InviteFriendsActivityPresenter
  private lateinit var browserRouter: ExternalBrowserRouter
  private var infoButtonSubject: PublishSubject<Any>? = null
  private var infoButtonInitializeSubject: ReplaySubject<Boolean>? = null

  @Inject
  lateinit var referralInteractor: ReferralInteractorContract

  @Inject
  lateinit var walletInteract: FindDefaultWalletInteract

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AndroidInjection.inject(this)
    setContentView(R.layout.invite_friends_activity_layout)
    toolbar()
    infoButtonSubject = PublishSubject.create()
    infoButtonInitializeSubject = ReplaySubject.create()
    browserRouter = ExternalBrowserRouter()
    navigateTo(LoadingFragment())
    presenter =
        InviteFriendsActivityPresenter(this, referralInteractor, walletInteract,
            CompositeDisposable(), Schedulers.io(), AndroidSchedulers.mainThread())
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> {
        super.onBackPressed()
      }
      R.id.action_info -> {
        infoButtonSubject?.onNext(Any())
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_info, menu)
    this.menu = menu
    infoButtonInitializeSubject?.onNext(true)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onResume() {
    super.onResume()
    presenter.present()
  }

  override fun navigateToVerificationFragment(amount: BigDecimal, currency: String) {
    hideNoNetworkView()
    navigateTo(InviteFriendsVerificationFragment.newInstance(amount, currency))
  }

  override fun navigateToInviteFriends(amount: BigDecimal, pendingAmount: BigDecimal,
                                       currency: String, link: String?, completed: Int,
                                       receivedAmount: BigDecimal, maxAmount: BigDecimal,
                                       available: Int, isRedeemed: Boolean) {
    hideNoNetworkView()
    navigateTo(InviteFriendsFragment.newInstance(amount, pendingAmount, currency, link, completed,
        receivedAmount, maxAmount, available, isRedeemed))
  }

  override fun getInfoButtonClick(): Observable<Any> {
    return infoButtonSubject!!
  }

  override fun infoButtonInitialized(): Observable<Boolean> {
    return infoButtonInitializeSubject!!
  }

  override fun showInfoButton() {
    this.menu.findItem(R.id.action_info)
        .isVisible = true
  }

  override fun navigateToWalletValidation(beenInvited: Boolean) {
    val intent = VerificationCreditCardActivity.newIntent(this)
        .apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
    startActivity(intent)
  }

  override fun showShare(link: String) {
    ShareCompat.IntentBuilder.from(this)
        .setText(link)
        .setType("text/plain")
        .setChooserTitle(resources.getString(R.string.referral_share_sheet_title))
        .startChooser()
  }

  private fun hideNoNetworkView() {
    fragment_container.visibility = VISIBLE
    no_network.visibility = GONE
  }

  private fun navigateTo(fragment: Fragment) {
    supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, fragment)
        .commit()
  }

  override fun retryClick(): Observable<Any> {
    return RxView.clicks(retry_button)
  }

  override fun showNetworkErrorView() {
    no_network.visibility = VISIBLE
    retry_button.visibility = VISIBLE
    retry_animation.visibility = GONE
    fragment_container.visibility = GONE
  }

  override fun showRetryAnimation() {
    retry_button.visibility = INVISIBLE
    retry_animation.visibility = VISIBLE
  }

  override fun onPause() {
    presenter.stop()
    super.onPause()
  }

  override fun onDestroy() {
    infoButtonSubject = null
    infoButtonInitializeSubject = null
    super.onDestroy()
  }

}