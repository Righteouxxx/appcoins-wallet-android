package cm.aptoide.skills

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.text.bold
import cm.aptoide.skills.databinding.FragmentSkillsBinding
import cm.aptoide.skills.entity.UserData
import cm.aptoide.skills.games.BackgroundGameService
import cm.aptoide.skills.interfaces.PaymentView
import cm.aptoide.skills.model.*
import cm.aptoide.skills.util.EskillsPaymentData
import cm.aptoide.skills.util.EskillsUriParser
import dagger.android.support.DaggerFragment
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class SkillsFragment : DaggerFragment(), PaymentView {

  companion object {
    fun newInstance() = SkillsFragment()

    private const val SESSION = "SESSION"
    private const val USER_ID = "USER_ID"
    private const val ROOM_ID = "ROOM_ID"
    private const val WALLET_ADDRESS = "WALLET_ADDRESS"

    private const val WALLET_CREATING_STATUS = "CREATING"
    private const val ESKILLS_URI_KEY = "ESKILLS_URI"

    private const val CLIPBOARD_TOOLTIP_DELAY_SECONDS = 3000L
  }

  @Inject
  lateinit var viewModel: SkillsViewModel

  @Inject
  lateinit var eskillsUriParser: EskillsUriParser

  private lateinit var disposable: CompositeDisposable
  private lateinit var binding: FragmentSkillsBinding

  override fun onCreateView(
      inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = FragmentSkillsBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    disposable = CompositeDisposable()

    requireActivity().onBackPressedDispatcher
        .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
          override fun handleOnBackPressed() {
            disposable.add(viewModel.cancelTicket()
                .subscribe { _, _ -> })
          }
        })
    disposable.add(viewModel.closeView()
        .subscribe { postbackUserData(it.first, it.second) })

    showPurchaseTicketLayout()
  }

  private fun showPurchaseTicketLayout() {
    val eSkillsPaymentData = getEskillsUri()
    setupPurchaseTicketLayout(eSkillsPaymentData)
    binding.payTicketLayout.root.visibility = View.VISIBLE
  }

  private fun setupPurchaseTicketLayout(
      eSkillsPaymentData: EskillsPaymentData) {
    setupQueueIdLayout()
    setupPurchaseTicketButtons(eSkillsPaymentData)
    setupAppNameAndIcon(eSkillsPaymentData.packageName)
    updateHeaderInfo(eSkillsPaymentData)
  }

  private fun setupQueueIdLayout() {
    binding.payTicketLayout.payTicketRoomDetails.openCardButton.setOnClickListener {
      if (binding.payTicketLayout.payTicketRoomDetails.roomCreateBody.visibility == View.GONE) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          binding.payTicketLayout.payTicketRoomDetails.createRoomTitle.setTextAppearance(
              R.style.DialogTitleStyle)
        } else {
          binding.payTicketLayout.payTicketRoomDetails.createRoomTitle.setTextAppearance(
              requireContext(),
              R.style.DialogTitleStyle)
        }
        binding.payTicketLayout.payTicketRoomDetails.openCardButton.rotation = 180F
        binding.payTicketLayout.payTicketRoomDetails.roomCreateBody.visibility = View.VISIBLE
      } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          binding.payTicketLayout.payTicketRoomDetails.createRoomTitle.setTextAppearance(
              R.style.DialogTextStyle)
        } else {
          binding.payTicketLayout.payTicketRoomDetails.createRoomTitle.setTextAppearance(
              requireContext(),
              R.style.DialogTextStyle)
        }
        binding.payTicketLayout.payTicketRoomDetails.openCardButton.rotation = 0F
        binding.payTicketLayout.payTicketRoomDetails.roomCreateBody.visibility = View.GONE
      }
    }
  }

  private fun setupPurchaseTicketButtons(
      eSkillsPaymentData: EskillsPaymentData) {
    binding.payTicketLayout.payTicketRoomDetails.copyButton.setOnClickListener {
      val queueId = binding.payTicketLayout.payTicketRoomDetails.roomId.text.toString()
      if (queueId.isNotEmpty()) {
        viewModel.saveQueueIdToClipboard(queueId.trim())
        val tooltip = binding.payTicketLayout.payTicketRoomDetails.tooltipClipboard
        tooltip.visibility = View.VISIBLE
        view?.postDelayed({ tooltip.visibility = View.GONE }, CLIPBOARD_TOOLTIP_DELAY_SECONDS)
      }
    }

    disposable.add(Single.zip(
        viewModel.getCreditsBalance(),
        viewModel.getFiatToAppcAmount(eSkillsPaymentData.price!!, eSkillsPaymentData.currency!!),
        { balance, appcAmount -> Pair(balance, appcAmount) })
        .observeOn(AndroidSchedulers.mainThread())
        .map {
          if (it.first < it.second.amount) {
            binding.payTicketLayout.dialogBuyButtonsPaymentMethods.buyButton.text =
                getString(R.string.topup_button)
            binding.payTicketLayout.dialogBuyButtonsPaymentMethods.buyButton.setOnClickListener {
              sendUserToTopUpFlow()
            }
          } else {
            binding.payTicketLayout.dialogBuyButtonsPaymentMethods.buyButton.setOnClickListener {
              val queueId = binding.payTicketLayout.payTicketRoomDetails.roomId.text.toString()
              if (queueId.isNotBlank()) {
                eSkillsPaymentData.queueId = QueueIdentifier(queueId.trim(), true)
              }
              binding.payTicketLayout.root.visibility = View.GONE
              createAndPayTicket(eSkillsPaymentData)
            }
          }
        }
        .subscribe())

    binding.payTicketLayout.dialogBuyButtonsPaymentMethods.cancelButton.setOnClickListener {
      viewModel.cancelPayment()
    }
  }

  private fun sendUserToTopUpFlow() {
    viewModel.sendUserToTopUpFlow(requireContext())
  }

  private fun setupAppNameAndIcon(packageName: String) {
    disposable.add(Single.fromCallable {
      viewModel.getApplicationInfo(packageName)
    }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ setHeaderInfo(it) }) { it.printStackTrace() })
  }

  private fun setHeaderInfo(applicationInfo: ApplicationInfo) {
    val header = binding.payTicketLayout.payTicketHeader
    header.appName.text = applicationInfo.name
    header.appIcon.setImageDrawable(applicationInfo.icon)
  }

  private fun updateHeaderInfo(eSkillsPaymentData: EskillsPaymentData) {
    val header = binding.payTicketLayout.payTicketHeader
    disposable.addAll(
        viewModel.getLocalFiatAmount(eSkillsPaymentData.price!!, eSkillsPaymentData.currency!!)
            .observeOn(AndroidSchedulers.mainThread())
            .map {
              header.fiatPrice.text = "${it.amount} ${it.currency}"
              header.fiatPriceSkeleton.visibility = View.GONE
              header.fiatPrice.visibility = View.VISIBLE
            }
            .subscribe(),
        viewModel.getFormattedAppcAmount(eSkillsPaymentData.price!!,
            eSkillsPaymentData.currency!!)
            .observeOn(AndroidSchedulers.mainThread())
            .map {
              header.appcPrice.text = "$it APPC"
              header.appcPriceSkeleton.visibility = View.GONE
              header.appcPrice.visibility = View.VISIBLE
            }
            .subscribe()
    )
  }

  private fun createAndPayTicket(eskillsPaymentData: EskillsPaymentData) {
    disposable.add(
        handleWalletCreationIfNeeded()
            .takeUntil { it != WALLET_CREATING_STATUS }
            .flatMapCompletable {
              viewModel.joinQueue(eskillsPaymentData)
                  .observeOn(AndroidSchedulers.mainThread())
                  .doOnSubscribe { showRoomLoading(false) }
                  .flatMapCompletable { handleTicketCreationResult(eskillsPaymentData, it) }
            }
            .subscribe()
    )
  }

  private fun handleTicketCreationResult(
      eskillsUri: EskillsPaymentData,
      ticket: Ticket
  ): Completable {
    return when (ticket) {
      is CreatedTicket -> purchaseTicket(eskillsUri, ticket)
      is FailedTicket -> Completable.fromAction { handleFailedTicketResult(ticket) }
      is PurchasedTicket -> return Completable.complete()
    }
  }

  private fun handleFailedTicketResult(ticket: FailedTicket) {
    when (ticket.status) {
      ErrorStatus.REGION_NOT_SUPPORTED -> showRegionNotSupportedError()
      ErrorStatus.NO_NETWORK -> showNoNetworkError()
      ErrorStatus.GENERIC -> showError()
    }
  }

  private fun purchaseTicket(
      eskillsUri: EskillsPaymentData,
      ticket: CreatedTicket
  ): Completable {
    return viewModel.getRoom(eskillsUri, ticket, this)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext { userData -> handleUserDataStatus(userData) }
        .ignoreElements()
  }

  private fun handleUserDataStatus(userData: UserData) {
    when (userData.status) {
      UserData.Status.IN_QUEUE, UserData.Status.PAYING -> showRoomLoading(true, userData.queueId)
      UserData.Status.REFUNDED -> showRefundedLayout()
      UserData.Status.COMPLETED -> postbackUserData(SkillsViewModel.RESULT_OK, userData)
      UserData.Status.FAILED -> showError()
    }
  }

  private fun showRegionNotSupportedError() {
    binding.loadingTicketLayout.root.visibility = View.GONE
    binding.refundTicketLayout.root.visibility = View.GONE
    binding.errorLayout.root.visibility = View.GONE
    binding.geofencingLayout.root.visibility = View.VISIBLE
    binding.geofencingLayout.okButton.setOnClickListener {
      finishWithError(SkillsViewModel.RESULT_REGION_NOT_SUPPORTED)
    }
  }

  private fun finishWithError(errorCode: Int) {
    requireActivity().setResult(errorCode)
    requireActivity().finish()
  }

  private fun showRefundedLayout() {
    binding.loadingTicketLayout.root.visibility = View.GONE
    binding.refundTicketLayout.root.visibility = View.VISIBLE
    binding.refundTicketLayout.refundOkButton.setOnClickListener { requireActivity().finish() }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == SkillsViewModel.AUTHENTICATION_REQUEST_CODE) {
      handleAuthenticationResult(resultCode)
    } else {
      super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onDestroyView() {
    disposable.clear()
    super.onDestroyView()
  }

  private fun getEskillsUri(): EskillsPaymentData {
    val intent = requireActivity().intent
    return eskillsUriParser.parse(Uri.parse(intent.getStringExtra(ESKILLS_URI_KEY)))
  }

  private fun handleWalletCreationIfNeeded(): Observable<String> {
    return viewModel.handleWalletCreationIfNeeded()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext {
          if (it == WALLET_CREATING_STATUS) {
            showWalletCreationLoadingAnimation()
          }
        }
        .filter { it != WALLET_CREATING_STATUS }
        .map {
          endWalletCreationLoadingAnimation()
          it
        }
  }

  private fun showWalletCreationLoadingAnimation() {
    binding.createWalletLayout.root.visibility = View.VISIBLE
    binding.createWalletLayout.createWalletAnimation.playAnimation()
  }

  private fun endWalletCreationLoadingAnimation() {
    binding.createWalletLayout.root.visibility = View.GONE
  }

  private fun showRoomLoading(isCancelActive: Boolean, queueIdentifier: QueueIdentifier? = null) {
    if (isCancelActive) {
      if (queueIdentifier != null && queueIdentifier.setByUser) {
        binding.loadingTicketLayout.loadingTitle.text = SpannableStringBuilder()
            .append(getString(R.string.finding_room_name_loading_title))
            .bold { append(" ${queueIdentifier.id}") }
      } else {
        binding.loadingTicketLayout.loadingTitle.text =
            getString(R.string.finding_room_loading_title)
      }
      binding.loadingTicketLayout.cancelButton.isEnabled = true
      binding.loadingTicketLayout.cancelButton.setOnClickListener {
        disposable.add(viewModel.cancelTicket()
            .subscribe { _, _ -> })
      }
    } else {
      binding.loadingTicketLayout.loadingTitle.text = getString(R.string.processing_loading_title)
    }
    binding.loadingTicketLayout.root.visibility = View.VISIBLE
  }

  private fun postbackUserData(resultCode: Int, userData: UserData) {
    if (resultCode == SkillsViewModel.RESULT_OK) {
      startBackgroundGameService(userData)
    }
    requireActivity().setResult(resultCode, buildDataIntent(userData))
    requireActivity().finish()
  }

  private fun buildDataIntent(userData: UserData): Intent {
    val intent = Intent()

    intent.putExtra(SESSION, userData.session)
    intent.putExtra(USER_ID, userData.userId)
    intent.putExtra(ROOM_ID, userData.roomId)
    intent.putExtra(WALLET_ADDRESS, userData.walletAddress?.address)

    return intent
  }

  private fun startBackgroundGameService(userData: UserData) {
    val intent = BackgroundGameService.newIntent(requireContext(), userData.session)
    context?.startService(intent)
  }

  override fun showLoading() {
    binding.geofencingLayout.root.visibility = View.GONE
    binding.refundTicketLayout.root.visibility = View.GONE
    binding.errorLayout.root.visibility = View.GONE
    binding.loadingTicketLayout.loadingTitle.text = getString(R.string.processing_payment_title)
    binding.loadingTicketLayout.root.visibility = View.VISIBLE
  }

  override fun hideLoading() {
    showRoomLoading(false)
  }

  override fun showError() {
    binding.loadingTicketLayout.root.visibility = View.GONE
    binding.refundTicketLayout.root.visibility = View.GONE
    binding.geofencingLayout.root.visibility = View.GONE
    binding.errorLayout.root.visibility = View.VISIBLE
    binding.errorLayout.errorOkButton.setOnClickListener {
      finishWithError(SkillsViewModel.RESULT_ERROR)
    }
  }

  override fun showFraudError(isVerified: Boolean) {
    if (!isVerified) {
      viewModel.sendUserToVerificationFlow(requireContext())
      finishWithError(SkillsViewModel.RESULT_ERROR)
    } else {
      showError()
    }
  }

  override fun showNoNetworkError() {
    binding.loadingTicketLayout.root.visibility = View.GONE
    binding.refundTicketLayout.root.visibility = View.GONE
    binding.errorLayout.root.visibility = View.GONE
    binding.noNetworkLayout.root.visibility = View.VISIBLE
    binding.noNetworkLayout.noNetworkOkButton.setOnClickListener {
      finishWithError(SkillsViewModel.RESULT_SERVICE_UNAVAILABLE)
    }
  }

  override fun showFingerprintAuthentication() {
    val intent = viewModel.getAuthenticationIntent(requireContext())
    startActivityForResult(intent, SkillsViewModel.AUTHENTICATION_REQUEST_CODE)
  }

  private fun handleAuthenticationResult(resultCode: Int) {
    if (resultCode == SkillsViewModel.RESULT_OK) {
      viewModel.restorePurchase(this)
          .subscribe()
    } else if (resultCode == SkillsViewModel.RESULT_USER_CANCELED) {
      viewModel.closeView()
    }
  }
}
