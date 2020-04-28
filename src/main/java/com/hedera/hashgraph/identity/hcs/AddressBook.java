package com.hedera.hashgraph.identity.hcs;

import com.google.gson.annotations.Expose;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import com.hedera.hashgraph.sdk.file.FileId;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Appent's address book for HCS identity network.
 */
public final class AddressBook {
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
  private AddressBook() {
  }

  /**
   * Converts this address book file into JSON string.
   *
   * @return The JSON representation of this address book.
   */
  public String toJson() {
    return JsonUtils.getGson().toJson(this);
  }

  /**
   * Converts an address book JSON string into address book object.
   *
   * @param  json              Address book JSON file.
   * @param  addressBookFileId FileId of this address book in Hedera File Service.
   * @return                   The {@link AddressBook}.
   */
  public static AddressBook fromJson(final String json, final @Nullable FileId addressBookFileId) {
    AddressBook result = JsonUtils.getGson().fromJson(json, AddressBook.class);
    result.setFileId(addressBookFileId);

    return result;
  }

  /**
   * Creates a new {@link AddressBook} instance. Does not create the file on Hedera File Service!.
   *
   * @param  appnetName       Name of the appnet.
   * @param  didTopicId       TopicID of the DID topic.
   * @param  vcTopicId        Topic ID of the Verifiable Credentials topic.
   * @param  appnetDidServers List of appnet API servers.
   * @return                  The {@link AddressBook}.
   */
  public static AddressBook create(
      final String appnetName,
      final String didTopicId,
      final String vcTopicId,
      final @Nullable List<String> appnetDidServers) {
    AddressBook result = new AddressBook();
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
