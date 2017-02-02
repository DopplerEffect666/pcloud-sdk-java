/*
 * Copyright (c) 2017 pCloud AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.pcloud.sdk.*;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class Main {

    public static void main(String... args) {
        String token = System.getenv("pcloud_token");
        ApiClient apiClient = PCloudSdk.newClientBuilder()
                .authenticator(Authenticators.newOAuthAuthenticator(token))
                .create();
        try {
            RemoteFolder folder = apiClient.listFolder(RemoteFolder.ROOT_FOLDER_ID, true).execute();
            printFolder(folder);

            RemoteFile newFile = uploadData(apiClient);
            printFileAttributes(newFile);

            FileLink downloadLink = apiClient.createFileLink(newFile, DownloadOptions.DEFAULT).execute();
            System.out.print(downloadLink.bestUrl());

            RemoteFile bigFile = uploadFile(apiClient, new File("some file path"));
            System.out.println(bigFile.createFileLink());
            downloadFile(bigFile, new File("some directory path"));

            UserInfo userInfo = apiClient.getUserInfo().execute();
            System.out.format(" User email: %s | Total quota %s | Used quota %s " , userInfo.email(), userInfo.totalQuota(), userInfo.usedQuota());


        } catch (IOException | ApiError e) {
            e.printStackTrace();
        }
    }

    private static void printFolder(RemoteFolder folder) throws IOException, ApiError {
        for (RemoteEntry entry : folder.children()) {
            printFileAttributes(entry);
        }
    }

    private static void printFileAttributes(RemoteEntry entry) {
        System.out.format("%s | Created:%s | Modified: %s | size:%s\n", entry.name(), entry.created(), entry.lastModified(), entry.isFile() ? String.valueOf(entry.asFile().size()) : "-");
    }

    private static RemoteFile uploadData(ApiClient apiClient) throws IOException, ApiError {
        String someName = UUID.randomUUID().toString();
        byte[] fileContents = someName.getBytes();
        return apiClient.createFile(RemoteFolder.ROOT_FOLDER_ID, someName + ".txt", DataSource.create(fileContents)).execute();
    }

    private static RemoteFile uploadFile(ApiClient apiClient, File file) throws IOException, ApiError {

        return apiClient.createFile(RemoteFolder.ROOT_FOLDER_ID, file.getName(), DataSource.create(file), new Date(file.lastModified()), new ProgressListener() {
            public void onProgress(long done, long total) {
                System.out.format("\rUploading... %.1f\n", ((double) done / (double) total) * 100d);
            }
        }).execute();
    }

    private static File downloadFile(RemoteFile remoteFile, File folder) throws IOException, ApiError {
        File destination = new File(folder, remoteFile.name());
        remoteFile.download(DataSink.create(destination), new ProgressListener() {
            public void onProgress(long done, long total) {
                System.out.format("\rDownloading... %.1f\n", ((double) done / (double) total) * 100d);
            }
        });
        return destination;
    }
}
