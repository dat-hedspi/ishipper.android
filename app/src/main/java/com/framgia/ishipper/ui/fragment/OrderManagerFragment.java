package com.framgia.ishipper.ui.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.framgia.ishipper.R;
import com.framgia.ishipper.common.Config;
import com.framgia.ishipper.model.Invoice;
import com.framgia.ishipper.model.Order;
import com.framgia.ishipper.model.ReviewUser;
import com.framgia.ishipper.model.User;
import com.framgia.ishipper.net.API;
import com.framgia.ishipper.net.APIDefinition;
import com.framgia.ishipper.net.APIResponse;
import com.framgia.ishipper.net.data.InvoiceData;
import com.framgia.ishipper.net.data.ReportUserData;
import com.framgia.ishipper.ui.activity.ListShipperRegActivity;
import com.framgia.ishipper.ui.activity.OrderDetailActivity;
import com.framgia.ishipper.ui.view.ReportDialog;
import com.framgia.ishipper.ui.view.ReviewDialog;
import com.framgia.ishipper.util.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class OrderManagerFragment extends Fragment implements OrderListFragment.OnActionClickListener {
    private static final String TAG = "OrderManagerFragment";
    private OrderManagerPagerAdapter mOrderManagerPagerAdapter;
    @BindView(R.id.viewpager) ViewPager mViewPager;
    @BindView(R.id.tabs) TabLayout mTabLayout;

    private List<OrderListFragment> mListOrderFragment;
    private Unbinder mUnbinder;
    private List<String> mOrderTitleList;
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_manager, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        initData();
        initEvent();
        return view;
    }

    private void initData() {
        mListOrderFragment = new ArrayList<>();

        mOrderManagerPagerAdapter =
                new OrderManagerPagerAdapter(getFragmentManager(), mListOrderFragment, mOrderTitleList);

        mViewPager.setAdapter(mOrderManagerPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
//        mViewPager.setOffscreenPageLimit(2);

        for (int type = 0; type < 7; type++) {
            //TODO: Fake list Order
            ArrayList<Invoice> invoices = new ArrayList<>();
            String titleTab;
            if (mOrderTitleList.size() > type) {
                titleTab = mOrderTitleList.get(type);
            } else {
                titleTab = getContext().getString(R.string.tab_title_unknow);
            }
            OrderListFragment orderListFragment =
                    OrderListFragment.newInstance(titleTab, type);
            orderListFragment.setOnActionClickListener(this);
            mListOrderFragment.add(orderListFragment);
            mOrderManagerPagerAdapter.notifyDataSetChanged();
        }

        mListOrderFragment.get(Invoice.STATUS_CODE_INIT).setData(mContext);

    }

    private void onSelectTab(int position) {
        mListOrderFragment.get(position).setData(mContext);
    }

    private void initEvent() {
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
                Log.d(TAG, "onPageScrolled: " + position);
            }

            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "onPageSelected: " + position);
                onSelectTab(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Log.d(TAG, "onPageScrollStateChanged: " + state);
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_order_manager, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public Context getContext() {
        return mContext;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public List<String> getOrderTitleList() {
        return mOrderTitleList;
    }

    public void setOrderTitleList(List<String> orderTitleList) {
        mOrderTitleList = orderTitleList;
    }

    public void notifyChangeTab(int type) {
        if (mListOrderFragment.size() > type) {
            mListOrderFragment.get(type).notifyChangedData(mContext);
        }
    }

    @Override
    public void onClickAction(final Invoice invoice) {
        final Dialog loadingDialog;
        switch (invoice.getStatusCode()) {
            case Invoice.STATUS_CODE_INIT:
                Intent intent = new Intent(mContext, ListShipperRegActivity.class);
                intent.putExtra(ListShipperRegActivity.KEY_INVOICE_ID, invoice.getId());
                startActivityForResult(intent, ListShipperRegActivity.REQUEST_CODE_RESULT);
                break;
            case Invoice.STATUS_CODE_WAITING:
                // TODO invoice id
                // Take invoice
                Log.d("hung", "onClickAction: take ");
                loadingDialog = CommonUtils.showLoadingDialog(getActivity());
                API.putUpdateInvoiceStatus(User.ROLE_SHIPPER, String.valueOf(invoice.getId()),
                        Config.getInstance().getUserInfo(getContext()).getAuthenticationToken(),
                        Invoice.STATUS_SHIPPING, new API.APICallback<APIResponse<InvoiceData>>() {
                            @Override
                            public void onResponse(APIResponse<InvoiceData> response) {
                                invoice.setStatus(Invoice.STATUS_SHIPPING);
                                notifyChangeTab(Invoice.STATUS_CODE_WAITING);
                                notifyChangeTab(Invoice.STATUS_CODE_SHIPPING);
                                loadingDialog.dismiss();
                            }

                            @Override
                            public void onFailure(int code, String message) {
                                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                                loadingDialog.dismiss();
                            }
                        });
                break;
            case Invoice.STATUS_CODE_SHIPPING:
                Log.d("hung", "onClickAction: shipping ");
                loadingDialog = CommonUtils.showLoadingDialog(getActivity());
                API.putUpdateInvoiceStatus(User.ROLE_SHIPPER, String.valueOf(invoice.getId()),
                        Config.getInstance().getUserInfo(getContext()).getAuthenticationToken(),
                        Invoice.STATUS_SHIPPED, new API.APICallback<APIResponse<InvoiceData>>() {
                            @Override
                            public void onResponse(APIResponse<InvoiceData> response) {
                                invoice.setStatus(Invoice.STATUS_SHIPPED);
                                notifyChangeTab(Invoice.STATUS_CODE_SHIPPING);
                                notifyChangeTab(Invoice.STATUS_CODE_SHIPPED);
                                loadingDialog.dismiss();
                            }

                            @Override
                            public void onFailure(int code, String message) {
                                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                                loadingDialog.dismiss();
                            }
                        });

                break;
            case Invoice.STATUS_CODE_SHIPPED:
                Log.d("hung", "onClickAction: delivered ");
                // Hoan thanh don hang da giao
                loadingDialog = CommonUtils.showLoadingDialog(getActivity());
                API.putUpdateInvoiceStatus(User.ROLE_SHOP, String.valueOf(invoice.getId()),
                        Config.getInstance().getUserInfo(getContext()).getAuthenticationToken(),
                        Invoice.STATUS_FINISHED, new API.APICallback<APIResponse<InvoiceData>>() {
                            @Override
                            public void onResponse(APIResponse<InvoiceData> response) {
                                invoice.setStatus(Invoice.STATUS_FINISHED);
                                notifyChangeTab(Invoice.STATUS_CODE_SHIPPED);
                                notifyChangeTab(Invoice.STATUS_CODE_FINISHED);
                                loadingDialog.dismiss();
                            }

                            @Override
                            public void onFailure(int code, String message) {
                                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                                loadingDialog.dismiss();
                            }
                        });
                break;
        }
    }


    @Override
    public void onClickCancel(Invoice invoice) {
                notifyChangeTab(invoice.getStatusCode());
                notifyChangeTab(Invoice.STATUS_CODE_CANCEL);
    }

    @Override
    public void onClickView(Invoice invoice) {
        Log.d("onClick item fragment", invoice.getAddressStart() + "null");
        Intent intent = new Intent(mContext, OrderDetailActivity.class);
        Bundle extras = new Bundle();
        extras.putInt(OrderDetailActivity.KEY_INVOICE_ID, invoice.getId());
        intent.putExtras(extras);
        startActivityForResult(intent, OrderDetailActivity.REQUEST_STATUS_CODE);
    }

    public class OrderManagerPagerAdapter extends FragmentPagerAdapter {

        private List<OrderListFragment> mOrderListFragments;
        private List<String> mListTitle;

        public OrderManagerPagerAdapter(FragmentManager fm, List<OrderListFragment> fragmentList,
                                        List<String> orderTitleList) {
            super(fm);
            mOrderListFragments = fragmentList;
            mListTitle = orderTitleList;
        }

        @Override
        public Fragment getItem(int position) {

            return mOrderListFragments.get(position);
        }

        @Override
        public int getCount() {
            if (mOrderListFragments == null)
                return 0;
            else
                return mOrderListFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mListTitle.get(position);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case ListShipperRegActivity.REQUEST_CODE_RESULT:
                    // TODO update list when accept success
                    break;
                case OrderDetailActivity.REQUEST_STATUS_CODE:
                    int mStatusCode = data.getIntExtra(OrderDetailActivity.KEY_STATUS_CODE, 0);
                    mViewPager.setCurrentItem(mStatusCode);
                    mListOrderFragment.get(mStatusCode).setData(mContext);
                    break;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}
