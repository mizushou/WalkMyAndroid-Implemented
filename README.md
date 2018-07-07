WalkMyAndroid - Starter Code
============

Starter code for the Walk My Android app which tracks location using the
Location API.

Introduction
------------
The starter code includes two layout files, one for landscape and one for
portrait mode, some images, and a predefined animation that will be used to
build up the final version.


Pre-requisites
--------------

You should be familiar with:
- Creating, building, and running apps in Android Studio.
- The Activity lifecycle.
- Persisting data across configuration changes.
- How to use an AsyncTask to do background work.
- Requesting permissions at runtime.


Getting Started
---------------

1. Download the code.
2. Open the code in Android Studio.
3. Run the app.

License
-------

Copyright 2017 Google, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.


Additional notes
------

This is a personal note.

##### 0. 前提
1. locationはただ端末の位置情報にアクセスするだけ。
2. place, mapのAPIと混合しないように
3. FusedLocationProviderClient.getLastLocation() method, which relies on other apps having already made location requests.　 だれかがすでに位置情報を取得している必要がある

##### 1. permissonの確認（location apiとは関係ない。汎用的な実装）
1. まずpermissonの確認を行い、なければ許可をユーザーに求めるようにする
    1. permissonが許可されていなかったら、ユーザーに求める
        1. ActivityCompat.requestPermissions() : ユーザーに許可を求める(本当にプロンプトが表示されて求める)
2. ActivityCompat.requestPermissions()に対する、イベントハンドラを実装する
    1. ユーザーからpermissionが与えられたら、もう一度getLocationを呼ぶ(ここの実装の仕方がうまい)
        1. イベントハンドラ : onRequestPermissionsResult()

##### 2. FusedLocationProviderClientで一番最新の位置情報を取得する
1. FusedLocationProviderClientのインスタンスを取得する
2. getLastLocation()メソッドを呼んで、イベントハンドラ（onSuccess(),,,）を実装する

##### 3. emulatorでテストするときに位置情報が取得できなくなった時の対処
1. Start the Google Maps app and accept the terms and conditions, if you haven't already.
1. Use the steps above to update the fused location provider. Google Maps will force the local cache to update.
1. Go back to your app and click the Get Location button. The app updates with the new location.

##### 4. 緯度経度情報を住所に変換する
1. 前提
    1. The process of converting from latitude/longitude to a physical address is called **reverse geocoding** .
    2. getFromLocation()@Geocoder class
        - 緯度経度 -> 住所(physical address)  : reverse geocoding
        - 住所 -> 緯度経度 : geocoding
        - The method is synchronous and requires a network connection
            - **network処理のくせして、同期**。そのため、このまま使うと使いにくい。（つまり、appのmainスレッドで呼ぶと固まる）
            - なので、非同期メソッドの中で呼ぶようにする  -> AsyncTask必要
1. 手順
    1. AsyncTaskのサブクラスを作成する（これActivityじゃないよ、ただのクラスだよ）
        - AsyncTaskは３つのジェネリック型で定義される
            ```
            AsyncTask objects are defined by three generic types:
            - Use the Params type to pass parameters into the doInBackground() method. For this app, the passed-in parameter is the Location object.
            - Use the Progress type to mark progress in the onProgressUpdate() method. For this app, you are not interested in the Progress type, because reverse geocoding is typically quick.
            - Use the Results type to publish results in the onPostExecute() method. For this app, the published result is the returned address String.
            ```
    2. doInBackground()メソッドを実装する(必須)
        1. ここの引数のタイプは、class定義の際に指定したものから来る
    3.  onPostExecute()メソッドを実装する（必須ではない）
        1. ここの引数のタイプは、class定義の際に指定したものから来る
    4. AsyncTaskのコンストラクタを定義する
        1. ここでも引数として、contextが必要
    5.  doInBackgroundを実際に実装する
        1. doInBackground(Location... locations)のlocationsはarray
        2. getFromLocation@Geocoderでrevese geocodingを実行
            1. 例外処理必要
        3. 住所に変換できた場合は、返却する
    6. onPostExecute()を実際に実装する
        1. 前提
            1. doInBackground() --String(address)--> onPostExecute()
                1. 自動で、doInBackgroundの戻り地はonPostExecuteに渡される。
            2.  doInBackgroundから結果が返ってきた時点で更新する
            3. 実装の前にAsyncTaskのサブクラスにインナークラスとしてinterfaceを用意する
                1. なぜそうするのか？
                    1. このinterfaceはdoInBackground()が返すresult(つまり変換されたaddress)を処理するメソッドonTaskCompleted(String result)を持つ。
                    2. このメソッド自体は、AsyncTackの中のonPostExecute()で呼ばれるが、実装自体はMainActivityで行う。ここがポイント。resultを処理するにはMainActivityの方が都合がいい。可読性も上がる気がする。たぶん（実装は都合の良いところで行う設計パターン）
                    3. そして、AsyncTaskの実装クラスのコンストラクターはこのinterfaceの実装クラスをmember変数として持てるようにしておく。これで準備OK。
                    4. MainActivityでinterfaceを実装して、AsyncTaskの実装クラスを実装クラスである自分自身を渡して完成。（これを自分で思いついて設計できる気がしない）
                    ```
                    new FetchAddressTask(MainActivity.this, MainActivity.this).execute(location);
                    ```
        2. interfaceをFetchAddressTaskクラスの中に定義する
        3. interfaceのmember変数を用意して、constructorでinterfaceを引数として取るようにする
        4. onPostExecute()の実装。ここで、member変数経由でinterfaceのメソッドを実行する。
            1. ここがポイント。これで実際はcompile type(MainActivity)である実装クラスの内容で実行される。
        5.  ここからはinterfaceの実装。MainActivityでinterfaceを実装する。
        6. そして、getLocationメソッドで位置情報を取得成功したら、非同期でreverse geocordingを行う。（executeメソッドから、doInBackgroundが呼ばれる）

            1. AsyncTaskのサブクラスをnewして、executeするだけ。

##### 5. 位置情報の更新定期的に受け取る
1. 前提
    1. ここまでは、他のアプリが取得した位置情報を取得しているだけ
    2. ここでは、このアプリ自身が位置情報を取得するようにする
        1. LocationRequest
            1. 今回は以下のパラメーターをセットする
                1. Interval
                    1. msec単位で更新間隔を設定する
                2. Fastest Interval
                    1. 更新間隔の最小値
                        1. Intervalで設定した値より更新情報を早く取得することがある。（他のアプリのリクエスト間隔が早いなど）
                        2. Intervalの間隔よ早く取得する際の最小値
                            1.  更新間隔 x : min < x < interval
                3. Priority
        2. LocationCallback
        3. Call requestLocationUpdates() on the FusedLocationProviderClient
2. 手順
    1. LocationRequestを作成するメソッドを用意する
    2. LocationCallbackをnewしてLocationCallbackのメンバー変数に入れる
        1. onLocationResultをoverride
    3.   周期的にLocation Requestをするための実装を行う
        1. startTrackingLocation内で、mFusedLocationClient.requestLocationUpdates()を呼ぶようにする
            1. このメソッドに、requestとcallbackを渡しておく
        2. stopTrackingLocation()内でも同様に、mFusedLocationClient.removeLocationUpdates(mLocationCallback)を呼ぶ
        3. 実際の処理をするcallbackを実装する
            1. Tracking stateがtrueであれば、非同期に投げる
            2. callback自体はmember変数に入れているので、update requestが通るたびにこのcallbackが呼ばれて、非同期でreverse geocodingを行う
3. emulatorでのテスト
    1. gpxファイルを読み込ませる。各位置情報の更新間隔を設定できる。
    2. [WalkMyAndroidPlaces-gpx](https://github.com/google-developer-training/android-advanced-starter-apps/tree/master/WalkMyAndroidPlaces-gpx)

##### 6. 省電力対策

1. Pauseになったら（Foucusがずれた時）、offにする。ただしmTrackingLocationは再度trueにしておく
2. Resumeになったら(Focusがあった時)、trackingを再開する

##### 7. tracking stateの永続化（rotation対策）

1. saved instanceで保存
