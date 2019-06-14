package com.example.oauth2.provider.scope.internal.spi;

import com.liferay.oauth2.provider.scope.spi.application.descriptor.ApplicationDescriptor;
import com.liferay.oauth2.provider.scope.spi.scope.descriptor.ScopeDescriptor;
import com.liferay.oauth2.provider.scope.spi.scope.finder.ScopeFinder;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.ResourceBundleLoader;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * @author Stian Sigvartsen
 */
@Component(
	immediate = true, property = {"dev.moode=true"},
	service = CustomApplicationScopeDescriptorsRegistrator.class
)
public class CustomApplicationScopeDescriptorsRegistrator {

	@Activate
	protected void activate(
			BundleContext bundleContext, Map<String, Object> properties)
		throws InvalidSyntaxException {

		_bundleContext = bundleContext;
		_devMode = GetterUtil.getBoolean(properties.get("dev.mode"), true);

		Set<String> overridableServices = new HashSet<>(_jaxRsApplicationNames);

		Object object = properties.get("osgi.jaxrs.name");

		if (object instanceof String[]) {
			List<String> osgiJaxrsNames = ListUtil.toList((String[])object);

			osgiJaxrsNames.forEach(
				osgiJaxrsName -> register(
					osgiJaxrsName, overridableServices.remove(osgiJaxrsName)));
		}
		else if (object instanceof String) {
			String osgiJaxrsName = (String)object;

			register(osgiJaxrsName, overridableServices.remove(osgiJaxrsName));
		}

		if (_devMode) {
			Stream<String> stream = _jaxRsApplicationNames.stream();

			stream.filter(
				overridableServices::contains
			).forEach(
				osgiJaxrsName -> register(osgiJaxrsName, true)
			);
		}
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC,
		policyOption = ReferencePolicyOption.GREEDY,
		target = "(&(osgi.jaxrs.name=*)(!(sap.scope.finder=true)))"
	)
	protected void addJaxRsApplicationName(
		ServiceReference<ScopeFinder> serviceReference) {

		_jaxRsApplicationNames.add(
			GetterUtil.getString(
				serviceReference.getProperty("osgi.jaxrs.name")));
	}

	@Deactivate
	protected void deactivate() {
		for (ServiceRegistration<?> serviceRegistration :
				_serviceRegistrations) {

			serviceRegistration.unregister();
		}
	}

	protected void register(String osgiJaxrsName, boolean overriding) {
		if (_log.isInfoEnabled()) {
			_log.info(
				"Publishing descriptor services for " + osgiJaxrsName +
					(overriding ? "" :
					" (not deployed, or not an JAX-RS app)"));
		}

		Dictionary<String, Object> properties = new HashMapDictionary<>();

		properties.put("osgi.jaxrs.name", osgiJaxrsName);
		properties.put("service.ranking", Integer.MAX_VALUE);

		_serviceRegistrations.add(
			_bundleContext.registerService(
				ApplicationDescriptor.class,
				locale -> {
					String key =
						"oauth2.application.description." + osgiJaxrsName;

					return (_devMode ? key + "=" : "") +
						GetterUtil.getString(
							ResourceBundleUtil.getString(
								_resourceBundleLoader.loadResourceBundle(
									locale),
								key),
							key);
				},
				properties));

		_serviceRegistrations.add(
			_bundleContext.registerService(
				ScopeDescriptor.class,
				(scope, locale) -> {
					String key = "oauth2.scope." + scope;

					return (_devMode ? key + "=" : "") +
						GetterUtil.getString(
							ResourceBundleUtil.getString(
								_resourceBundleLoader.loadResourceBundle(
									locale),
								key),
							_defaultScopeDescriptor.describeScope(
								scope, locale));
				},
				properties));
	}

	protected void removeJaxRsApplicationName(
		ServiceReference<ScopeFinder> serviceReference) {

		_jaxRsApplicationNames.remove(
			GetterUtil.getString(
				serviceReference.getProperty("osgi.jaxrs.name")));
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CustomApplicationScopeDescriptorsRegistrator.class);

	private BundleContext _bundleContext;

	@Reference(
		policy = ReferencePolicy.DYNAMIC,
		policyOption = ReferencePolicyOption.GREEDY, target = "(default=true)"
	)
	private volatile ScopeDescriptor _defaultScopeDescriptor;

	private boolean _devMode = true;
	private final Set<String> _jaxRsApplicationNames =
		Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Reference(
		target = "(bundle.symbolic.name=com.example.oauth2.provider.scope)"
	)
	private ResourceBundleLoader _resourceBundleLoader;

	private volatile List<ServiceRegistration<?>> _serviceRegistrations =
		new ArrayList<>();

}