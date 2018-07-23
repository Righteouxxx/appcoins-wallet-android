package com.asfoundation.wallet.ui.iab;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.asf.wallet.R;
import dagger.android.support.DaggerFragment;

/**
 * Created by franciscocalado on 20/07/2018.
 */

public class ExpressCheckoutBuyFragment extends DaggerFragment implements ExpressCheckoutBuyView {
  Bundle extras;
  String data;

  public static ExpressCheckoutBuyFragment newInstance(Bundle extras, String uri) {
    ExpressCheckoutBuyFragment fragment = new ExpressCheckoutBuyFragment();
    Bundle bundle = new Bundle();
    bundle.putBundle("extras", extras);
    bundle.putString("data", uri);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    extras = getArguments().getBundle("extras");
    data = getArguments().getString("data");


  }

  @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.fragment_express_checkout_buy, container, false);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
  }

  @Override public void onDestroy() {
    super.onDestroy();
  }
}
