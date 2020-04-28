package com.hedera.hashgraph.identity.hcs.vc;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

public final class IssuerTypeAdapterFactory implements TypeAdapterFactory {
  @SuppressWarnings("unchecked")
  @Override
  public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
    if (type.getRawType() != Issuer.class) {
      return null;
    }
    return (TypeAdapter<T>) new IssuerTypeAdapter(gson.getAdapter(Issuer.class), gson.getAdapter(String.class));
  }

  private static final class IssuerTypeAdapter extends TypeAdapter<Issuer> {
    private final TypeAdapter<Issuer> delegateAdapter;
    private final TypeAdapter<String> elementAdapter;

    IssuerTypeAdapter(final TypeAdapter<Issuer> delegateAdapter, final TypeAdapter<String> elementAdapter) {
      this.delegateAdapter = delegateAdapter;
      this.elementAdapter = elementAdapter;
    }

    @Override
    public Issuer read(final JsonReader reader) throws IOException {
      if (reader.peek() == JsonToken.STRING) {
        return new Issuer(elementAdapter.read(reader));
      }
      return delegateAdapter.read(reader);
    }

    @Override
    public void write(final JsonWriter writer, final Issuer value)
        throws IOException {
      if (value != null && value.getName() == null) {
        elementAdapter.write(writer, value.getId());
      } else {
        delegateAdapter.write(writer, value);
      }
    }
  }
}