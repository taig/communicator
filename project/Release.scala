import sbt.Keys._
import sbt.{Project, State}
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations
import sbtrelease.ReleaseStateTransformations._

object Release {
    /**
     * Specify release version, not asking for a next version in case of a snapshot release
     */
    lazy val inquireVersions: ReleaseStep = { state: State =>
        val extracted = Project.extract( state )

        val useDefs = state.get( useDefaults ).getOrElse( false )
        val currentV = extracted.get( version )

        val releaseFunc = extracted.get( releaseVersion )
        val suggestedReleaseV = releaseFunc( currentV )

        //flatten the Option[Option[String]] as the get returns an Option, and the value inside is an Option
        val releaseV = readVersion(
            suggestedReleaseV,
            "Release version [%s] : ",
            useDefs,
            state.get( commandLineReleaseVersion ).flatten
        )

        val nextV = if( releaseV endsWith "-SNAPSHOT" ) {
            releaseV
        } else {
            val nextFunc = extracted.get( releaseNextVersion )
            val suggestedNextV = nextFunc( releaseV )
            //flatten the Option[Option[String]] as the get returns an Option, and the value inside is an Option
            readVersion(
                suggestedNextV,
                "Next version [%s] : ",
                useDefs,
                state.get( commandLineNextVersion ).flatten
            )
        }

        state.put( versions, ( releaseV, nextV ) )
    }

    lazy val commitReleaseVersion: ReleaseStep = { state: State =>
        val extracted = Project.extract( state )
        val releaseV = extracted.get( version )

        if( releaseV endsWith "-SNAPSHOT" ) {
            state.log.info( "Skipping 'commitReleaseVersion' for snapshot release" )
            state
        } else {
            ReleaseStateTransformations.commitReleaseVersion( state )
        }
    }

    lazy val tagRelease: ReleaseStep = { state: State =>
        val extracted = Project.extract( state )
        val releaseV = extracted.get( version )
        
        if( releaseV endsWith "-SNAPSHOT" ) {
            state.log.info( "Skipping 'tagRelease' for snapshot release" )
            state
        } else {
            ReleaseStateTransformations.tagRelease( state )
        }
    }

    lazy val setNextVersion: ReleaseStep = { state: State =>
        val ( current, next ) = state.get( versions ).get
        
        if( current == next ) {
            state.log.info( "Skipping 'setNextVersion' because current and next version are equal" )
            state
        } else {
            ReleaseStateTransformations.setNextVersion( state )
        }
    }

    lazy val commitNextVersion: ReleaseStep = { state: State =>
        val extracted = Project.extract( state )
        val releaseV = extracted.get( version )

        if( releaseV endsWith "-SNAPSHOT" ) {
            state.log.info( "Skipping 'commitNextVersion' for snapshot release" )
            state
        } else {
            ReleaseStateTransformations.commitNextVersion( state )
        }
    }

    lazy val pushChanges: ReleaseStep = { state: State =>
        val extracted = Project.extract( state )
        val releaseV = extracted.get( version )

        if( releaseV endsWith "-SNAPSHOT" ) {
            state.log.info( "Skipping 'pushChanges' for snapshot release" )
            state
        } else {
            ReleaseStateTransformations.pushChanges( state )
        }
    }
}