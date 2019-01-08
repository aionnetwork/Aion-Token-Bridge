/* eslint-disable */
import React, {Component} from 'react';
import NCNonIdealState from 'components/common/NCNonIdealState';
import NCLoading from 'components/common/NCLoading';

export default class NCExplorerSection extends Component
{
  render() {
    const { 
      className,

      content,

      isLoading,
      loadingTitle,

      isError,
      errorNonIdealState=null,

      paddingTop=null,
      paddingBottom=null
    } = this.props;


    const defaultErrorIcon="pt-icon-warning-sign";
    const defaultErrorTitle="Unknown Error Occurred";
    const defaultErrorDesc="Please try again later.";
    const defaultErrorShowHomeLink=true;

    let renderedNonIdealState = errorNonIdealState;

    if (renderedNonIdealState == null) {
      renderedNonIdealState = <NCNonIdealState
        paddingTop={paddingTop}
        paddingBottom={paddingBottom}
        icon={defaultErrorIcon}
        title={defaultErrorTitle}
        description={defaultErrorDesc}
        showHomeLink={defaultErrorShowHomeLink}/>;
    }

    return(
      <div className={className}>
      {
        (isLoading) &&
        <NCLoading
          title={loadingTitle}
          marginTop={paddingTop}
          marginBottom={paddingBottom}/>
      }
      {
        (!isLoading && isError) && renderedNonIdealState
      }
      {
        (!isLoading && !isError) && content
      }
      </div>
    );
  }
}
