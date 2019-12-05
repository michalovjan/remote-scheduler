package org.jboss.pnc.scheduler.facade;

import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.core.ServiceContainerImpl;
import org.jboss.pnc.scheduler.core.api.BatchServiceInstaller;
import org.jboss.pnc.scheduler.core.api.ServiceBuilder;
import org.jboss.pnc.scheduler.core.api.ServiceRegistry;
import org.jboss.pnc.scheduler.core.api.ServiceTarget;
import org.jboss.pnc.scheduler.core.model.Service;
import org.jboss.pnc.scheduler.dto.ServiceDTO;
import org.jboss.pnc.scheduler.facade.api.ServiceProvider;
import org.jboss.pnc.scheduler.facade.mapper.ServiceMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ServiceProviderImpl implements ServiceProvider {

    private ServiceTarget target;

    private ServiceRegistry registry;

    private ServiceMapper mapper;

    //CDI
    @Deprecated
    public ServiceProviderImpl() {
    }

    @Inject
    public ServiceProviderImpl(ServiceContainerImpl container, ServiceMapper mapper) {
        this.target = container;
        this.registry = container;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public List<ServiceDTO> create(List<ServiceDTO> services) {
        BatchServiceInstaller batchServiceInstaller = target.addServices();
        for (ServiceDTO service : services) {
            Service serviceModel = mapper.toDB(service);
            ServiceBuilder builder = batchServiceInstaller.addService(serviceModel.getName())
                    .setPayload(serviceModel.getPayload())
                    .setInitialMode(Mode.ACTIVE)
                    .setRemoteEndpoints(serviceModel.getRemoteEndpoints());
            serviceModel.getDependencies().forEach(builder::requires);
            serviceModel.getDependants().forEach(builder::isRequiredBy);
            builder.install();
        }
        batchServiceInstaller.commit();
        List<ServiceDTO> toReturn = new ArrayList<>();
        for (ServiceDTO serviceDTO : services) {
            toReturn.add(mapper.toDTO(registry.getRequiredService(ServiceName.parse(serviceDTO.getName()))));
        }
        return toReturn;
    }

    @Override
    public List<ServiceDTO> getAll(boolean waiting, boolean running, boolean finished) {
        return registry.getServices(waiting,running,finished).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancel(ServiceName serviceName) {
        registry.getRequiredServiceController(serviceName).setMode(Mode.CANCEL);
    }

    @Override
    public ServiceDTO get(ServiceName serviceName) {
        return mapper.toDTO(registry.getService(serviceName));
    }

    @Override
    public List<ServiceDTO> getAllRelated(ServiceName serviceName) {
        return null;
    }

    @Override
    @Transactional
    public void acceptRemoteResponse(ServiceName serviceName, boolean positive) {
        if (positive) {
            registry.getRequiredServiceController(serviceName).accept();
        } else {
            registry.getRequiredServiceController(serviceName).fail();
        }
    }
}
