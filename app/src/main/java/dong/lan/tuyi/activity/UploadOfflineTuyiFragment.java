package dong.lan.tuyi.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UploadFileListener;
import dong.lan.tuyi.R;
import dong.lan.tuyi.adapter.OfflineAdapter;
import dong.lan.tuyi.adapter.UserMainAdapter;
import dong.lan.tuyi.bean.UserTuyi;
import dong.lan.tuyi.db.DemoDBManager;
import dong.lan.tuyi.db.OfflineTuyi;
import dong.lan.tuyi.utils.Config;
import dong.lan.tuyi.xlist.XListView;

/**
 * Created by Dooze on 2015/8/22.
 */
public class UploadOfflineTuyiFragment extends Fragment implements XListView.IXListViewListener, AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener, View.OnClickListener {

    private XListView mListView;
    private TextView upload, null_tip;
    private List<UserTuyi> offTuyis = new ArrayList<UserTuyi>();
    private List<String> offTuyisUrl = new ArrayList<>();
    private OfflineAdapter adapter;
    private LinearLayout uploadLayout;
    private TextView tip;
    public static boolean hasChange = false;
    private Map<Integer, Integer> map = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_offline_tuyi_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity().getIntent().hasExtra("FROM_DESK")) {
            OfflineTuyi offlineTuyi = new OfflineTuyi(getActivity());
        }
        mListView = (XListView) getView().findViewById(R.id.offline_list);
        mListView.setOnItemClickListener(this);
        mListView.setPullLoadEnable(false);
        mListView.setPullRefreshEnable(true);
        mListView.setXListViewListener(this);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
        mListView.pullRefreshing();
        upload = (TextView) getView().findViewById(R.id.offline_upload);
        null_tip = (TextView) getView().findViewById(R.id.offline_null_tip);
        TextView bar_center = (TextView) getView().findViewById(R.id.bar_center);
        TextView bar_left = (TextView) getView().findViewById(R.id.bar_left);
        TextView bar_right = (TextView) getView().findViewById(R.id.bar_right);
        uploadLayout = (LinearLayout) getView().findViewById(R.id.upload_tip_layout);
        tip = (TextView) getView().findViewById(R.id.upload_tip_text);
        uploadLayout.setVisibility(View.GONE);
        bar_right.setOnClickListener(this);
        bar_left.setOnClickListener(this);
        bar_center.setText(getString(R.string.local_recode));
        bar_right.setText(getString(R.string.upload));
        upload.setOnClickListener(this);
        initData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasChange) {
            hasChange = false;
            initData();
            null_tip.setVisibility(View.GONE);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hasChange && !hidden) {
            hasChange = false;
            initData();
            map.clear();
            null_tip.setVisibility(View.GONE);
        }

    }

    private void initData() {
        offTuyis = DemoDBManager.getInstance().getAllOfflineTuyi();
        if (offTuyis == null) {
            null_tip.setVisibility(View.VISIBLE);
        } else {
            adapter = new OfflineAdapter(getActivity(), offTuyis);
            mListView.setAdapter(adapter);
            mListView.setPullLoadEnable(false);
            for (int i = 0; i < offTuyis.size(); i++) {
                if (!Config.tUser.getUsername().equals(offTuyis.get(i).getOfflineNmae())) {
                    offTuyis.remove(i);
                    adapter.remove(i);
                }
            }
            adapter.notifyDataSetChanged();
            for (int i = 0, size = offTuyis.size(); i < size; i++) {
                offTuyisUrl.add(offTuyis.get(i).gettUri());
            }
        }
    }

    @Override
    public void onRefresh() {
        if (adapter.getCount() < DemoDBManager.getInstance().getTuyiCount()) {
            adapter = new OfflineAdapter(getActivity(), offTuyis);
            mListView.setAdapter(adapter);
        }

    }

    @Override
    public void onLoadMore() {

    }

    android.app.AlertDialog alertDialog;

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setCancelable(true);
        alertDialog.show();
        alertDialog.getWindow().setContentView(R.layout.offline_item_delete_dialog);
        TextView cancel = (TextView) alertDialog.findViewById(R.id.offline_dialog_cancel);
        TextView delete = (TextView) alertDialog.findViewById(R.id.offline_dialog_delete);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DemoDBManager.getInstance().deleteOffTuyiByTime(adapter.getList().get(position - 1).getTime());
                adapter.remove(position - 1);
                mListView.deferNotifyDataSetChanged();
                alertDialog.dismiss();
            }
        });
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(new Intent(getActivity(), ReEditTuyiActivity.class).putExtra("OFFLINE_TUYI", adapter.getList().get(position - 1)));
        getActivity().overridePendingTransition(R.anim.slide_out_to_left, R.anim.slide_in_from_right);
    }

    boolean run = false;
    boolean load = true;
    boolean save = false;
    String url = "";
    Thread thread;
    UserTuyi tuyi = null;

    List<String> urls = new ArrayList<String>();

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bar_left:
                getActivity().finish();
                break;
            case R.id.bar_right:
            case R.id.offline_upload:

                if (Config.tUser == null) {
                    Config.Show(getActivity(),getString(R.string.user_asyn_no_finish));
                    return;
                }
                if (offTuyis == null) {
                    Config.Show(getActivity(), getString(R.string.no_offline_tuyi));
                    return;
                }

                if (run) {
                    return;
                } else {
                    run = true;
                }

//                BmobFile.uploadBatch(getActivity(), (String[]) offTuyisUrl.toArray(), new UploadBatchListener() {
//                    @Override
//                    public void onSuccess(List<BmobFile> list, List<String> list1) {
//                        offTuyisUrl.removeAll(list1);
//                        String remain = "剩余"+offTuyisUrl.size()+"个图忆未上传成功";
//                        tip.setText(remain);
//
//
//                        run = false;
//                    }
//
//                    @Override
//                    public void onProgress(int curIndex, int curPercent, int total,int totalPercent) {
//                        String progress = "当前上传 "+total +" 中的第 "+curIndex +" 个，当前进度："+curPercent+"%";
//                        tip.setText(progress);
//                    }
//
//                    @Override
//                    public void onError(int i, String s) {
//                        tip.setText(s);
//                        run = false;
//                    }
//                });


                urls.clear();
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (adapter.getList().get(i).gettPoint() != null && !map.containsKey(i)) {
                        map.put(i, i);
                    }
                }
                if (run) {
                    return;
                } else {
                    run = true;
                }
                uploadLayout.setVisibility(View.VISIBLE);
                tip.setText(R.string.start_upload);
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (run) {
                            if (map.size() < 1) {
                                run = false;
                                load = false;
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        uploadLayout.setVisibility(View.GONE);
                                    }
                                });
                                return;
                            }
                            if (load) {
                                load = false;
                                final BmobFile bmobFile = new BmobFile(new File(offTuyis.get(map.get(map.size() - 1)).gettUri()));
                                bmobFile.upload(getActivity(), new UploadFileListener() {
                                    @Override
                                    public void onSuccess() {
                                        save = true;
                                        String info = "离线图忆 " + offTuyis.get(map.get(map.size() - 1)).gettContent() + "的图片上传成功";
                                        tip.setText(info);
                                        urls.add(bmobFile.getFileUrl(getActivity()));
                                    }

                                    @Override
                                    public void onFailure(int i, String s) {
                                        load = true;
                                        tip.setText(s);
                                    }
                                });
                            }
                            if (save && !urls.isEmpty() && urls.get(urls.size() - 1) != null && !urls.get(urls.size() - 1).equals("")) {
                                save = false;
                                tuyi = offTuyis.get(map.get(map.size() - 1));
                                tuyi.setZan(0);
                                tuyi.settUser(Config.tUser);
                                tuyi.settPic(urls.get(urls.size() - 1));
                                tuyi.save(getActivity(), new SaveListener() {
                                    @Override
                                    public void onSuccess() {
                                        if (map.size() > 0)
                                            Config.Show(getActivity(), offTuyis.get(map.get(map.size() - 1)).gettContent() + "保存成功");
                                        DemoDBManager.getInstance().deleteOffTuyiByTime(offTuyis.get(map.get(map.size() - 1)).getTime());
                                        tip.setText(getString(R.string.offline_tuyi_upload_tip_text, offTuyis.get(map.get(map.size() - 1)).gettContent()));
                                        offTuyis.get(map.get(map.size() - 1)).settPic(urls.get(urls.size() - 1));
                                        offTuyis.get(map.get(map.size() - 1)).settUser(Config.tUser);
                                        DemoDBManager.getInstance().saveTuyi(tuyi);
                                        adapter.remove(map.get(map.size() - 1));
                                        map.remove(map.size() - 1);
                                        load = map.size() >= 1;
                                        mListView.deferNotifyDataSetChanged();
                                        Config.updateStatus(getActivity(), offTuyis.get(map.get(map.size() - 1)).getTAG());
                                    }

                                    @Override
                                    public void onFailure(int i, String s) {
                                        save = true;
                                        tip.setText("离线图忆 " + offTuyis.get(map.get(map.size() - 1)).gettContent() + " 上传失败：" + s);

                                    }
                                });
                            }
                        }
                    }
                });
                thread.start();
                break;
        }
    }

    @Override
    public void onDestroy() {
        run = false;
        load = false;
        thread = null;
        UserMainAdapter.FLAG = UserMainAdapter.USERMAIN;
        super.onDestroy();
    }
}
