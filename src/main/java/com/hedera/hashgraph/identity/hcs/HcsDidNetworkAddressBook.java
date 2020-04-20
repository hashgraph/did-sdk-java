package com.hedera.hashgraph.identity.hcs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.hedera.hashgraph.sdk.file.FileId;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Appent's address book for HCS identity network.
 */
public final class HcsDidNetworkAddressBook {
  @Expose(serialize = false, deserialize = false)
  private FileId fileId;

  @Expose(serialize = true, deserialize = true)
  private String appnetName;

  @Expose(serialize = true, deserialize = true)
  private String didTopicId;

  @Expose(serialize = true, deserialize = true)
  private String vcTopicId;

  @Expose(serialize = true, deserialize = true)
  private List<String> appnetDidServers;

  /**
   * Default constructor.
   */
  private HcsDidNetworkAddressBook() {
  }

  /**
   * Converts this address book file into JSON string.
   *
   * @return The JSON representation of this address book.
   */
  public String toJson() {
    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().disableHtmlEscaping().create();
    return gson.toJson(this);
  }

  /**
   * Converts an address book JSON string into address book object.
   *
   * @param  json              Address book JSON file.
   * @param  addressBookFileId FileId of this address book in Hedera File Service.
   * @return                   The {@link HcsDidNetworkAddressBook}.
   */
  public static HcsDidNetworkAddressBook fromJson(final String json, final @Nullable FileId addressBookFileId) {
    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    HcsDidNetworkAddressBook result = gson.fromJson(json, HcsDidNetworkAddressBook.class);
    result.setFileId(addressBookFileId);

    return result;
  }

  /**
   * Creates a new {@link HcsDidNetworkAddressBook} instance. Does not create the file on Hedera File Service!.
   *
   * @param  appnetName       Name of the appnet.
   * @param  didTopicId       TopicID of the DID topic.
   * @param  vcTopicId        Topic ID of the Verifiable Credentials topic.
   * @param  appnetDidServers List of appnet API servers.
   * @return                  The {@link HcsDidNetworkAddressBook}.
   */
  public static HcsDidNetworkAddressBook create(
      final String appnetName,
      final String didTopicId,
      final String vcTopicId,
      final @Nullable List<String> appnetDidServers) {
    HcsDidNetworkAddressBook result = new HcsDidNetworkAddressBook();
    result.appnetDidServers = appnetDidServers;
    result.didTopicId = didTopicId;
    result.vcTopicId = vcTopicId;
    result.appnetName = appnetName;

    return result;
  }

  public String getAppnetName() {
    return appnetName;
  }

  public void setAppnetName(final String appnetName) {
    this.appnetName = appnetName;
  }

  public String getDidTopicId() {
    return didTopicId;
  }

  public void setDidTopicId(final String didTopicId) {
    this.didTopicId = didTopicId;
  }

  public String getVcTopicId() {
    return vcTopicId;
  }

  public void setVcTopicId(final String vcTopicId) {
    this.vcTopicId = vcTopicId;
  }

  public List<String> getAppnetDidServers() {
    return appnetDidServers;
  }

  public void setAppnetDidServers(final List<String> appnetDidServers) {
    this.appnetDidServers = appnetDidServers;
  }

  public FileId getFileId() {
    return fileId;
  }

  public void setFileId(final FileId fileId) {
    this.fileId = fileId;
  }
}
