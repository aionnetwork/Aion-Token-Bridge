/* eslint-disable */
import React, {Component} from 'react';
import NCComponentLazyLoad from 'components/common/NCComponentLazyLoad';
import NCExplorerSection from 'components/common/NCExplorerSection';

export default class NCExplorerPage extends Component
{
  render() {
    const { 
      page,

      isLoading=false,
      loadingTitle="Loading",

      isError=false,
      errorNonIdealState=null
    } = this.props;

    return (
      <NCComponentLazyLoad>
        <NCExplorerSection
          className="NCExplorerPage"
          content={page}

          isLoading={isLoading}
          loadingTitle={loadingTitle}

          isError={isError}
          errorNonIdealState={errorNonIdealState}
          paddingTop={140}
          paddingBottom={40}/>
      </NCComponentLazyLoad>
    );
  }
}