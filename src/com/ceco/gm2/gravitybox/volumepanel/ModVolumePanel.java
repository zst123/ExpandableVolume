/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.gm2.gravitybox.volumepanel;

import java.lang.reflect.Field;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ModVolumePanel implements IXposedHookLoadPackage {
	public static final String PACKAGE_NAME = "android";
	private static final String CLASS_VOLUME_PANEL = "android.view.VolumePanel";
	private static final String CLASS_STREAM_CONTROL = "android.view.VolumePanel$StreamControl";
	
	private static Object mVolumePanel;
	private static boolean mExpandable;
	private static boolean mExpandFully;
	
	public static void init(final ClassLoader classLoader) {
		try {
			final Class<?> classVolumePanel = XposedHelpers.findClass(CLASS_VOLUME_PANEL,
					classLoader);
			final Class<?> classStreamControl = XposedHelpers.findClass(CLASS_STREAM_CONTROL,
					classLoader);
			
			XposedBridge.hookAllConstructors(classVolumePanel, new XC_MethodHook() {
				
				@Override
				protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
					mVolumePanel = param.thisObject;
					mExpandable = true;
					mExpandFully = true;
					updateVolumePanelMode();
				}
			});
			
			XposedHelpers.findAndHookMethod(classVolumePanel, "expand", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
					hideNotificationSliderIfLinked();
					
					if (!mExpandFully) return;
					View mMoreButton = (View) XposedHelpers.getObjectField(mVolumePanel,
							"mMoreButton");
					View mDivider = (View) XposedHelpers.getObjectField(mVolumePanel, "mDivider");
					
					if (mMoreButton != null) {
						mMoreButton.setVisibility(View.GONE);
					}
					
					if (mDivider != null) {
						mDivider.setVisibility(View.GONE);
					}
				}
			});
			
			try {
				final Field fldVolTitle = XposedHelpers.findField(classStreamControl, "volTitle");
 				XposedBridge.hookAllConstructors(classStreamControl, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
						Context context = (Context) XposedHelpers.getObjectField(
								XposedHelpers.getSurroundingThis(param.thisObject), "mContext");
						if (context != null) {
							TextView tv = new TextView(context);
							fldVolTitle.set(param.thisObject, tv);
						}
					}
				});
			} catch (Throwable t) {
			}
		} catch (Throwable t) {
			XposedBridge.log(t);
		}
	}
	
	private static void updateVolumePanelMode() {
		if (mVolumePanel == null) return;
		
		View mMoreButton = (View) XposedHelpers.getObjectField(mVolumePanel, "mMoreButton");
		View mDivider = (View) XposedHelpers.getObjectField(mVolumePanel, "mDivider");
		
		if (mMoreButton != null) {
			mMoreButton.setVisibility(mExpandable ? View.VISIBLE : View.GONE);
			if (!mMoreButton.hasOnClickListeners()) {
				mMoreButton.setOnClickListener((OnClickListener) mVolumePanel);
			}
		}
		
		if (mDivider != null) {
			mDivider.setVisibility(mExpandable ? View.VISIBLE : View.GONE);
		}
		
		XposedHelpers.setBooleanField(mVolumePanel, "mShowCombinedVolumes", mExpandable);
		XposedHelpers.setObjectField(mVolumePanel, "mStreamControls", null);
	}
	
	private static final int STREAM_NOTIFICATION = 5;
	
	private static void hideNotificationSliderIfLinked() {
		if (mVolumePanel == null) return;
		
		@SuppressWarnings("unchecked")
		Map<Integer, Object> streamControls = (Map<Integer, Object>) XposedHelpers.getObjectField(
				mVolumePanel, "mStreamControls");
		if (streamControls == null) return;
		
		for (Object o : streamControls.values()) {
			if ((Integer) XposedHelpers.getIntField(o, "streamType") == STREAM_NOTIFICATION) {
				View v = (View) XposedHelpers.getObjectField(o, "group");
				if (v != null) {
					v.setVisibility(View.GONE);
					break;
				}
			}
		}
	}
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		init(lpparam.classLoader);
		
	}
}