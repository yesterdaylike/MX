package com.black.mx;

import java.io.File;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;

public class XActivity extends Activity {
	private ListView mInstalledAppListView = null;
	private PackageManager pm = null;
	private String[] packageNames;
	private String[] apkFiles;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_x);

		mInstalledAppListView = (ListView)findViewById(R.id.install_listview);
		mInstalledAppListView.setOnItemClickListener(mAllAppItemClickListener);
		pm = getPackageManager();
		new LoadApkFilesTask().execute();
	}

	OnItemClickListener mAllAppItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			// TODO Auto-generated method stub
			TextView tvtag = (TextView) view.findViewById(R.id.option);
			Log.e("apk", String.valueOf(tvtag.getText()));

			try {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(new File(tvtag.getText().toString())), "application/vnd.android.package-archive");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				// TODO: handle exception
			}
		}
	};

	private class LoadApkFilesTask extends AsyncTask<Void, Integer, Void> {

		private List<String> mAllFileArray;
		private List<Map<String, Object>> list = null;

		protected Void doInBackground(Void... urls) {
			File IntMemory_Path = new File("/mnt/sdcard");
			/*File SDCard_Path = new File("/mnt/external_sd");
			File USB_Path = new File("/mnt/usb_storage");*/
			File Youmi_Path = new File("/storage/emulated/0/Android/data/.youmicache");
			String extension = ".apk";

			mAllFileArray = new ArrayList<String>();
			getFiles(mAllFileArray, IntMemory_Path, extension, true);
			getFilesYoumi(mAllFileArray, Youmi_Path);
			/*getFiles(mAllFileArray, SDCard_Path, extension, true);
			getFiles(mAllFileArray, USB_Path, extension, true);*/

			if( mAllFileArray.size() > 0 ){ 
				Collections.sort(mAllFileArray, new ApkFileSort());
				list = getAllAppData(mAllFileArray);
			}
			return null;
		}

		protected void onPostExecute(Void result) {

			if( null == list || list.size() < 1 ){
				return;
			}

			SimpleAdapter mApkFileAdapter = new SimpleAdapter(XActivity.this,
					list,
					R.layout.apkfile_listitem,
					new String[] { "icon", "title", "summary", "option", "tag"},
					new int[] {R.id.icon, R.id.title, R.id.summary, R.id.option,R.id.tag});
			mInstalledAppListView.setAdapter(mApkFileAdapter);

			mApkFileAdapter.setViewBinder(new ViewBinder(){
				public boolean setViewValue(View view,Object data,String textRepresentation){
					if(view instanceof ImageView && data instanceof Drawable){
						ImageView iv=(ImageView)view;
						iv.setImageDrawable((Drawable)data);
						return true;
					}
					else return false;
				}
			});
		}
	}

	//搜索目录，扩展名，是否进入子文件夹
	public void getFiles(List<String> list, File Path, String Extension, boolean IsIterative){
		File[] files = Path.listFiles();
		if( null == files || files.length < 1 ){
			return;
		}
		for (File file : files) {
			if (file.isFile()){
				String filePath = file.getPath();

				if (filePath.toLowerCase().endsWith(Extension))  //判断扩展名
					list.add(file.getPath());

				if (!IsIterative)
					break;
			}
			else if (file.isDirectory() && file.getPath().indexOf("/.") == -1)  //忽略点文件（隐藏文件/文件夹）
				getFiles(list, file, Extension, IsIterative);
		}
	}
	//搜索目录，扩展名，是否进入子文件夹
	public void getFilesYoumi(List<String> list, File Path ){
		File[] files = Path.listFiles();
		if( null == files || files.length < 1 ){
			return;
		}
		for (File file : files) {
			if (file.isFile()){
				list.add(file.getPath());
			}
			else if (file.isDirectory())
				getFilesYoumi(list, file);
		}
	}

	public class ApkFileSort implements Comparator<String>{
		private Collator collator = Collator.getInstance(); //调入这个是解决中文排序问题 
		private Map<String, CollationKey> map = new HashMap<String, CollationKey>();
		private CollationKey lkey;
		private CollationKey rkey;
		private PackageInfo pkgInfo;
		private String label;
		private Object object;

		@Override
		public int compare(String lhs, String rhs) {
			// TODO Auto-generated method stub
			object = map.get(lhs);
			if( null != object ){
				lkey = (CollationKey) object;
			}
			else{
				pkgInfo = pm.getPackageArchiveInfo(lhs,PackageManager.GET_ACTIVITIES);
				if( null == pkgInfo ){
					Log.i("zhengwenhui", "null == pkgInfo "+lhs);
					lkey = collator.getCollationKey(lhs.toLowerCase());
					map.put(lhs, lkey);
				}
				else{
					label = (String) pkgInfo.applicationInfo.loadLabel(pm);
					lkey = collator.getCollationKey(label.toLowerCase());
					map.put(lhs, lkey);
				}
			}

			object = map.get(rhs);
			if( null != object ){
				rkey = (CollationKey) object;
			}
			else{
				pkgInfo = pm.getPackageArchiveInfo(rhs,PackageManager.GET_ACTIVITIES);
				if( null == pkgInfo ){
					Log.i("zhengwenhui", "null == pkgInfo "+rhs);
					rkey = collator.getCollationKey(rhs.toLowerCase());
					map.put(rhs, rkey);
				}
				else{
					label = (String) pkgInfo.applicationInfo.loadLabel(pm);
					rkey = collator.getCollationKey(label.toLowerCase());
					map.put(rhs, rkey);
				}
			}
			return lkey.compareTo(rkey);
		}
	}

	private List<Map<String, Object>> getAllAppData(List<String> appFiles) {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map;

		PackageInfo pkgInfo;

		packageNames = new String[appFiles.size()];
		apkFiles = new String[appFiles.size()];
		for (int i = 0; i < appFiles.size(); i++) {
			String appfile = appFiles.get(i);
			//}
			//for (String appfile : appFiles) {
			pkgInfo = pm.getPackageArchiveInfo(appfile,PackageManager.GET_ACTIVITIES);  
			if (pkgInfo != null) {
				ApplicationInfo appInfo = pkgInfo.applicationInfo;
				/* 必须加这两句，不然下面icon获取是default icon而不是应用包的icon */
				appInfo.sourceDir = appfile;
				appInfo.publicSourceDir = appfile;

				long lengthInBytes = new File(appfile).length();
				String length = humanReadableByteCount(lengthInBytes, true);

				map = new HashMap<String, Object>();
				map.put("icon", appInfo.loadIcon(pm));
				map.put("title", appInfo.loadLabel(pm));
				map.put("summary", pkgInfo.versionName+"     "+length);
				map.put("option", appfile);
				map.put("tag", appInfo.packageName);

				packageNames[i] = appInfo.packageName;
				apkFiles[i] = appfile;

				list.add(map);
			}
		}
		return list;
	}

	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.x, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if( null != apkFiles && apkFiles.length > 0){


			switch (item.getItemId()) {
			case R.id.action_install:
				for (String file : apkFiles) {
					Log.i("install", file);
					Toast.makeText(this, "install: "+file, Toast.LENGTH_SHORT).show();
					install(file);
				}
				break;
			case R.id.action_open:
				for (String pkg : packageNames) {
					Log.i("open", pkg);
					Toast.makeText(this, "open: "+pkg, Toast.LENGTH_SHORT).show();
					start(pkg);
				}
				break;
			case R.id.action_uninstall:
				for (String pkg : packageNames) {
					Log.i("uninstall", pkg);
					Toast.makeText(this, "uninstall: "+pkg, Toast.LENGTH_SHORT).show();
					uninstall(pkg);
				}
				break;
			case R.id.action_delete:
				for (String file : apkFiles) {
					Log.i("delete", file);
					Toast.makeText(this, "delete: "+file, Toast.LENGTH_SHORT).show();
					delete(file);
				}
				break;

			default:
				break;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	public void start(String packageName){
		Intent LaunchIntent = pm.getLaunchIntentForPackage(packageName);
		if( null != LaunchIntent){
			startActivity( LaunchIntent );
		}
		else{
			Log.e("Intent", "can_not_open_apk Intent is null!");
		}
	}

	public void install(String file) {
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(new File(file)), "application/vnd.android.package-archive");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			// TODO: handle exception
		}
	}

	public void uninstall(String packageName){
		Uri packageUri = Uri.parse("package:" + packageName);
		Intent deleteIntent = new Intent();
		deleteIntent.setAction(Intent.ACTION_DELETE);
		deleteIntent.setData(packageUri);
		startActivity(deleteIntent);
	}

	public void delete(String filepath){

		File file=new File(filepath);
		if(file.exists()){
			file.delete();
		}
	}
}
