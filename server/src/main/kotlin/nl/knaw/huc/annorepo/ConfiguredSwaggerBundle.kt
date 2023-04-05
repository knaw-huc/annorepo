package nl.knaw.huc.annorepo

import io.dropwizard.core.ConfiguredBundle
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

class ConfiguredSwaggerBundle<T>() :
    ConfiguredBundle<AnnoRepoConfiguration> {

}
