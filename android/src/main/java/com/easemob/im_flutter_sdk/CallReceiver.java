/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.easemob.im_flutter_sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hyphenate.util.EMLog;

import java.util.HashMap;
import java.util.Map;

public class CallReceiver extends BroadcastReceiver implements EMWrapper {

	@Override
	public void onReceive(Context context, Intent intent) {

		System.out.println("CallReceiver----------------------------------------------------");
		//username
		String from = intent.getStringExtra("from");
		//call type
		String type = intent.getStringExtra("type");
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("from",from);
		//video call
		post((Void)->{
			EMChatManagerWrapper.channel.invokeMethod("onCallReceive",data);
		});
		EMLog.d("CallReceiver", "app received a incoming call");
	}

}
