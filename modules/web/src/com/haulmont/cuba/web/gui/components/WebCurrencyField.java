/*
 * Copyright (c) 2008-2017 Haulmont.
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

package com.haulmont.cuba.web.gui.components;

import com.haulmont.bali.util.Preconditions;
import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.datatypes.DatatypeRegistry;
import com.haulmont.chile.core.datatypes.ValueConversionException;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.app.dynamicattributes.DynamicAttributesUtils;
import com.haulmont.cuba.core.entity.annotation.CurrencyValue;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.components.CurrencyField;
import com.haulmont.cuba.gui.components.data.ConversionException;
import com.haulmont.cuba.gui.components.data.meta.EntityValueSource;
import com.haulmont.cuba.gui.data.Datasource;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;

public class WebCurrencyField<V extends Number> extends WebV8AbstractField<CubaCurrencyField, String, V>
        implements CurrencyField<V> {

    protected Locale locale;
    protected Datatype<V> datatype;
    protected Datatype<V> defaultDatatype;

    public WebCurrencyField() {
        component = new CubaCurrencyField();
        component.setCurrencyLabelPosition(toWidgetLabelPosition(CurrencyLabelPosition.RIGHT));

        attachValueChangeListener(component);
    }

    @Inject
    public void setDatatypeRegistry(DatatypeRegistry datatypeRegistry) {
        //noinspection unchecked
        this.defaultDatatype = (Datatype<V>) datatypeRegistry.get(BigDecimal.class);
    }

    @Override
    protected void attachValueChangeListener(CubaCurrencyField component) {
        component.getInternalComponent()
                .addValueChangeListener(event ->
                        componentValueChanged(event.getOldValue(), event.getValue(), event.isUserOriginated()));
    }

    @Inject
    public void setUserSessionSource(UserSessionSource userSessionSource) {
        this.locale = userSessionSource.getLocale();
    }

    @Override
    protected String convertToPresentation(V modelValue) throws ConversionException {
        Datatype<V> datatype = getDatatypeInternal();
        // Vaadin TextField does not permit `null` value
        if (datatype != null) {
            return nullToEmpty(datatype.format(modelValue, locale));
        }

        if (valueBinding != null
                && valueBinding.getSource() instanceof EntityValueSource) {
            EntityValueSource entityValueSource = (EntityValueSource) valueBinding.getSource();
            Datatype<V> propertyDataType = entityValueSource.getMetaPropertyPath().getRange().asDatatype();
            return nullToEmpty(propertyDataType.format(modelValue, locale));
        }

        return nullToEmpty(super.convertToPresentation(modelValue));
    }

    @Override
    protected V convertToModel(String componentRawValue) throws ConversionException {
        String value = StringUtils.trimToNull(emptyToNull(componentRawValue));

        Datatype<V> datatype = getDatatypeInternal();
        if (datatype != null) {
            try {
                return datatype.parse(value, locale);
            } catch (ValueConversionException e) {
                throw new ConversionException(e.getLocalizedMessage(), e);
            } catch (ParseException e) {
                throw new ConversionException(getConversionErrorMessage(), e);
            }
        }

        if (valueBinding != null
                && valueBinding.getSource() instanceof EntityValueSource) {
            EntityValueSource entityValueSource = (EntityValueSource) valueBinding.getSource();
            Datatype<V> propertyDataType = entityValueSource.getMetaPropertyPath().getRange().asDatatype();
            try {
                return propertyDataType.parse(componentRawValue, locale);
            } catch (ValueConversionException e) {
                throw new ConversionException(e.getLocalizedMessage(), e);
            } catch (ParseException e) {
                throw new ConversionException(getConversionErrorMessage(), e);
            }
        }

        return super.convertToModel(componentRawValue);
    }

    protected String getConversionErrorMessage() {
        Messages messages = beanLocator.get(Messages.NAME);
        return messages.getMainMessage("databinding.conversion.error");
    }

    @Override
    public void setCurrency(String currency) {
        component.setCurrency(currency);
    }

    @Override
    public String getCurrency() {
        return component.getCurrency();
    }

    @Override
    public void setShowCurrencyLabel(boolean showCurrencyLabel) {
        component.setShowCurrencyLabel(showCurrencyLabel);
    }

    @Override
    public boolean getShowCurrencyLabel() {
        return component.getShowCurrencyLabel();
    }

    @Override
    public void setCurrencyLabelPosition(CurrencyLabelPosition currencyLabelPosition) {
        Preconditions.checkNotNullArgument(currencyLabelPosition);

        component.setCurrencyLabelPosition(toWidgetLabelPosition(currencyLabelPosition));
    }

    @Override
    public CurrencyLabelPosition getCurrencyLabelPosition() {
        return fromWidgetLabelPosition(component.getCurrencyLabelPosition());
    }

    @Override
    public void setDatasource(Datasource datasource, String property) {
        super.setDatasource(datasource, property);

        // vaadin8 support in value source
        if (datasource != null && !DynamicAttributesUtils.isDynamicAttribute(property)) {
            MetaProperty metaProperty = datasource.getMetaClass()
                    .getPropertyNN(property);

            Object annotation = metaProperty.getAnnotations()
                    .get(CurrencyValue.class.getName());
            if (annotation == null) {
                return;
            }

            //noinspection unchecked
            Map<String, Object> annotationProperties = (Map<String, Object>) annotation;

            String currencyName = (String) annotationProperties.get("currency");
            component.setCurrency(currencyName);

            String labelPosition = ((com.haulmont.cuba.core.entity.annotation.CurrencyLabelPosition) annotationProperties.get("labelPosition")).name();
            setCurrencyLabelPosition(CurrencyLabelPosition.valueOf(labelPosition));
        }
    }

    @Override
    public void setDatatype(Datatype<V> datatype) {
        Preconditions.checkNotNullArgument(datatype);

        this.datatype = datatype;
    }

    @Override
    public Datatype<V> getDatatype() {
        return datatype;
    }

    protected Datatype<V> getDatatypeInternal() {
        if (datatype != null) {
            return datatype;
        }
        return valueBinding == null ? defaultDatatype : null;
    }

    @Override
    public void commit() {
        super.commit();
    }

    @Override
    public void discard() {
        super.discard();
    }

    @Override
    public boolean isBuffered() {
        return super.isBuffered();
    }

    @Override
    public void setBuffered(boolean buffered) {
        super.setBuffered(buffered);
    }

    @Override
    public boolean isModified() {
        return super.isModified();
    }

    protected com.haulmont.cuba.web.widgets.CurrencyLabelPosition toWidgetLabelPosition(CurrencyLabelPosition labelPosition) {
        return com.haulmont.cuba.web.widgets.CurrencyLabelPosition.valueOf(labelPosition.name());
    }

    protected CurrencyLabelPosition fromWidgetLabelPosition(com.haulmont.cuba.web.widgets.CurrencyLabelPosition wLabelPosition) {
        return CurrencyLabelPosition.valueOf(wLabelPosition.name());
    }
}
