package com.chargeroute.app.domain.usecase

import com.chargeroute.app.domain.model.NavStep
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.repository.RouteRepository

class GetNavigationStepsUseCase(
    private val routeRepository: RouteRepository
) {
    suspend operator fun invoke(plan: RoutePlan): List<NavStep> {
        return routeRepository.getNavigationSteps(plan)
    }
}
