package com.hedera.hashgraph.identity.utils;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public final class SingleToArrayTypeAdapterFactory<E> implements TypeAdapterFactory {
  @SuppressWarnings("unchecked")
  @Override
  public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
    if (type.getRawType() != List.class) {
      return null;
    }

    Type elementType = ((ParameterizedType) type.getType()).getActualTypeArguments()[0];
    Type listType = TypeToken.get(List.class).getType();
    TypeToken<List<T>> listTypeToken = (TypeToken<List<T>>) TypeToken.getParameterized(listType, elementType);

    TypeAdapter<List<T>> delegateAdapter = gson.getAdapter(listTypeToken);
    TypeAdapter<T> elementAdapter = (TypeAdapter<T>) gson.getAdapter(TypeToken.get(elementType));

    return (TypeAdapter<T>) new SingleToArrayTypeAdapter<>(delegateAdapter, elementAdapter);
  }

  private final class SingleToArrayTypeAdapter<T> extends TypeAdapter<List<T>> {
    private final TypeAdapter<List<T>> delegateAdapter;
    private final TypeAdapter<T> elementAdapter;

    SingleToArrayTypeAdapter(final TypeAdapter<List<T>> delegateAdapter,
        final TypeAdapter<T> elementAdapter) {
      this.delegateAdapter = delegateAdapter;
      this.elementAdapter = elementAdapter;
    }

    @Override
    public List<T> read(final JsonReader reader) throws IOException {
      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
        return Collections.singletonList(elementAdapter.read(reader));
      }
      return delegateAdapter.read(reader);
    }

    @Override
    public void write(final JsonWriter writer, final List<T> value)
        throws IOException {
      if (value.size() == 1) {
        elementAdapter.write(writer, value.get(0));
      } else {
        delegateAdapter.write(writer, value);
      }
    }
  }
}